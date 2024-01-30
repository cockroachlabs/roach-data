package io.roach.data.jdbc.plain;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.time.Duration;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionTemplate {
    private static final Logger logger = LoggerFactory.getLogger(TransactionTemplate.class);

    public static <T> T execute(DataSource ds,
                                TransactionCallback<T> action) {
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);

            T result;
            try {
                result = action.doInTransaction(conn);
            } catch (RuntimeException | Error ex) {
                conn.rollback();
                throw ex;
            } catch (Throwable ex) {
                conn.rollback();
                throw new UndeclaredThrowableException(ex,
                        "TransactionCallback threw undeclared checked exception");
            }
            conn.commit();
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public static <T> T executeWithRetries(DataSource ds,
                                           TransactionCallback<T> action) {
        int maxCalls = 10;
        for (int n = 1; n <= maxCalls; n++) {
            try (Connection conn = ds.getConnection()) {
                conn.setAutoCommit(false);

                try {
                    T result = action.doInTransaction(conn);
                    conn.commit();
                    return result;
                } catch (SQLException ex) {
                    if ("40001".equals(ex.getSQLState())) {
                        conn.rollback();
                        handleTransientException(ex, n, maxCalls);
                    } else {
                        throw ex;
                    }
                } catch (Throwable ex) {
                    conn.rollback();
                    throw new UndeclaredThrowableException(ex,
                            "TransactionCallback threw undeclared checked exception");
                }
            } catch (SQLException e) {
                throw new DataAccessException("Failed to connect", e);
            }
        }
        throw new DataAccessException("Too many transient errors - giving up");
    }

    public static <T> T executeWithSavepointRetries(DataSource ds,
                                                    TransactionCallback<T> action) {
        int maxCalls = 10;

        for (int n = 1; n <= maxCalls; n++) {
            try (Connection conn = ds.getConnection()) {
                conn.setAutoCommit(false);

                try {
                    Savepoint savepoint = conn.setSavepoint("cockroach_restart");

                    T result;

                    for (int i = 0; ; i++) {
                        if (i > 10) {
                            throw new DataAccessException("Too many transient errors - giving up");
                        }

                        try {
                            result = action.doInTransaction(conn);
                            conn.releaseSavepoint(savepoint);
                            break;
                        } catch (SQLException e) {
                            conn.rollback(savepoint);
                            if ("40001".equals(e.getSQLState())) {
                                handleTransientException(e, n, maxCalls);
                            } else {
                                throw e;
                            }
                        }
                    }

                    conn.commit();
                    return result;
                } catch (SQLException ex) {
                    if ("40001".equals(ex.getSQLState())) {
                        conn.rollback();
                        handleTransientException(ex, n, maxCalls);
                    } else {
                        conn.rollback();
                        throw ex;
                    }
                } catch (Throwable ex) {
                    conn.rollback();
                    throw new UndeclaredThrowableException(ex,
                            "TransactionCallback threw undeclared checked exception");
                }
            } catch (SQLException e) {
                throw new DataAccessException(e);
            }
        }
        throw new DataAccessException("Too many transient errors - giving up");
    }

    public static final Duration MAX_BACKOFF = Duration.ofSeconds(15);

    private static void handleTransientException(SQLException sqlException, int numCalls, int maxCalls) {
        try {
            long backoffMillis = Math.min((long) (Math.pow(2, numCalls) + Math.random() * 1000),
                    MAX_BACKOFF.toMillis());
            if (numCalls <= 1 && logger.isWarnEnabled()) {
                logger.warn("Transient SQL error (%s) in call %d/%d (backoff for %d ms before retry): %s"
                        .formatted(sqlException.getSQLState(),
                                numCalls,
                                maxCalls,
                                backoffMillis,
                                sqlException.getMessage()));
            }
            Thread.sleep(backoffMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
