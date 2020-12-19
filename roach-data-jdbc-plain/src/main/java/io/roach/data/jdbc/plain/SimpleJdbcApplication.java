package io.roach.data.jdbc.plain;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

public class SimpleJdbcApplication {
    private static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }

    private static class DataAccessException extends RuntimeException {
        public DataAccessException(String message) {
            super(message);
        }

        public DataAccessException(Throwable cause) {
            super(cause);
        }
    }

    private interface TransactionCallback<T> {
        T doInTransaction(Connection conn) throws SQLException;
    }

    private static class TransactionTemplate {
        private TransactionTemplate() {
        }

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
    }

    private interface ConnectionCallback<T> {
        T doInConnection(Connection conn) throws SQLException;
    }

    private static class ConnectionTemplate {
        private ConnectionTemplate() {
        }

        public static <T> T execute(DataSource ds,
                                    ConnectionCallback<T> action) {
            try (Connection conn = ds.getConnection()) {
                T result;
                try {
                    result = action.doInConnection(conn);
                } catch (RuntimeException | Error ex) {
                    throw ex;
                } catch (Throwable ex) {
                    throw new UndeclaredThrowableException(ex,
                            "TransactionCallback threw undeclared checked exception");
                }
                return result;
            } catch (SQLException e) {
                throw new DataAccessException(e);
            }
        }
    }

    private static class AccountLeg {
        String name;

        BigDecimal amount;

        AccountLeg(String name, BigDecimal amount) {
            this.name = name;
            this.amount = amount;
        }
    }

    private static TransactionCallback<Void> transfer(List<AccountLeg> legs) {
        return conn -> {
            BigDecimal checksum = BigDecimal.ZERO;

            for (AccountLeg leg : legs) {
                BigDecimal balance = readBalanceForUpdate(conn, leg.name);
                if (balance.add(leg.amount).compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessException("Insufficient balance: " + leg.name);
                }
                updateBalance(conn, leg.name, leg.amount);
                checksum = checksum.add(leg.amount);
            }

            if (checksum.compareTo(BigDecimal.ZERO) != 0) {
                throw new BusinessException(
                        "Sum of account legs must equal 0 (got " + checksum.toPlainString() + ")"
                );
            }

            return null;
        };
    }

    private static List<String> readAccountNames(Connection conn) throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT name FROM t_account")) {
            try (ResultSet res = ps.executeQuery()) {
                while (res.next()) {
                    names.add(res.getString(1));
                }
            }
        }
        return names;
    }

    private static BigDecimal readBalanceForUpdate(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT balance FROM t_account WHERE name = ? FOR UPDATE")) {
            ps.setString(1, name);

            try (ResultSet res = ps.executeQuery()) {
                if (!res.next()) {
                    throw new BusinessException("Account not found: " + name);
                }
                return res.getBigDecimal("balance");
            }
        }
    }

    private static void updateBalance(Connection conn, String name, BigDecimal delta) throws SQLException {
        try (PreparedStatement ps = conn
                .prepareStatement(
                        "UPDATE t_account SET balance = balance+?, updated=clock_timestamp() where name = ?")) {
            ps.setBigDecimal(1, delta);
            ps.setString(2, name);
            if (ps.executeUpdate() != 1) {
                throw new DataAccessException("Rows affected != 1  for " + name);
            }
        }
    }

    private static BigDecimal readTotalBalance(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "select sum(balance) balance from t_account AS OF SYSTEM TIME '-5s'")) {
            try (ResultSet res = ps.executeQuery()) {
                if (!res.next()) {
                    throw new SQLException("Empty result");
                }
                return res.getBigDecimal("balance");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        final HikariDataSource hikariDS = new HikariDataSource();
        hikariDS.setJdbcUrl("jdbc:postgresql://localhost:26257/roach_data?sslmode=disable");
        hikariDS.setUsername("root");
        hikariDS.setAutoCommit(true);

        boolean skipProxy = Arrays.stream(args).anyMatch(arg -> arg.equals("--skip-proxy"));

        DataSource ds = skipProxy ? hikariDS :
                ProxyDataSourceBuilder
                        .create(hikariDS)
                        .name("SQL-Trace")
                        .asJson()
                        .logQueryToSysOut()
                        .build();

        setupSchema(ds);

        ConnectionTemplate.execute(ds, SimpleJdbcApplication::readAccountNames).forEach(name -> {
            System.out.printf("Balance before for %s: %s\n", name,
                    ConnectionTemplate.execute(ds, c -> readBalanceForUpdate(c, name)));
        });

        System.out.printf("Total balance before: %s\n",
                ConnectionTemplate.execute(ds, SimpleJdbcApplication::readTotalBalance));

        List<AccountLeg> legs = new ArrayList<>();
        legs.add(new AccountLeg("customer:a", new BigDecimal("0.02")));
        legs.add(new AccountLeg("customer:b", new BigDecimal("0.11").negate()));
        legs.add(new AccountLeg("customer:c", new BigDecimal("0.09")));
        legs.add(new AccountLeg("customer:d", new BigDecimal("5.01").negate()));
        legs.add(new AccountLeg("customer:e", new BigDecimal("5.01")));

        // Run concurrently for more exiting effects
        final ExecutorService executorService = Executors.newCachedThreadPool();
        final Deque<Future<Object>> futures = new ArrayDeque<>();

        IntStream.rangeClosed(1, 10).forEach(
                value -> futures.add(executorService.submit(() -> TransactionTemplate.execute(ds, transfer(legs)))));

        while (!futures.isEmpty()) {
            try {
                futures.pop().get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                e.printStackTrace(System.err);
            }
        }

        executorService.shutdownNow();

        ConnectionTemplate.execute(ds, SimpleJdbcApplication::readAccountNames).forEach(name -> {
            System.out.printf("Balance after for %s: %s\n", name,
                    ConnectionTemplate.execute(ds, c -> readBalanceForUpdate(c, name)));
        });

        System.out.printf("Total balance after: %s\n",
                ConnectionTemplate.execute(ds, SimpleJdbcApplication::readTotalBalance));
    }

    private static void setupSchema(DataSource ds) throws Exception {
        URL sql = SimpleJdbcApplication.class.getResource("/db/create.sql");

        StringBuilder buffer = new StringBuilder();

        Files.readAllLines(Paths.get(sql.toURI())).forEach(line -> {
            if (!line.startsWith("--") && !line.isEmpty()) {
                buffer.append(line);
            }
            if (line.endsWith(";") && buffer.length() > 0) {
                ConnectionTemplate.execute(ds, conn -> {
                    try (Statement statement = conn.createStatement()) {
                        statement.execute(buffer.toString());
                    }
                    buffer.setLength(0);
                    return null;
                });
            }
        });

        Thread.sleep(5000); // Let schema change take effect
    }
}
