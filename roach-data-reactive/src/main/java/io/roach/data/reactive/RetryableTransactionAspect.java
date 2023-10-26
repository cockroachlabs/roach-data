package io.roach.data.reactive;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import io.r2dbc.postgresql.api.ErrorDetails;
import io.r2dbc.postgresql.api.PostgresqlException;
import reactor.core.publisher.Mono;

@Component
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class RetryableTransactionAspect {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private int retryAttempts = 30;

    private int maxBackoff = 15000;

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public void setMaxBackoff(int maxBackoff) {
        this.maxBackoff = maxBackoff;
    }

    @Pointcut("execution(* io.roach..*(..)) && @annotation(transactional)")
    public void anyTransactionBoundaryOperation(Transactional transactional) {
    }

    @Around(value = "anyTransactionBoundaryOperation(transactional)",
            argNames = "pjp,transactional")
    public Object doInTransaction(ProceedingJoinPoint pjp, Transactional transactional)
            throws Throwable {
        Assert.isTrue(!TransactionSynchronizationManager.isActualTransactionActive(),
                "Expecting NO active transaction - check advice @Order and @EnableTransactionManagement order");

        int methodCalls = 0;
        ErrorDetails retryCause = null;

        final Instant callTime = Instant.now();

        do {
            final Throwable throwable;
            try {
                methodCalls++;

                Object rv = pjp.proceed();

                if (methodCalls > 1) {
                    handleExceptionRecovery(retryCause, methodCalls, pjp.getSignature(),
                            Duration.between(callTime, Instant.now()));
                }

                // Anti-reactive but \_(ツ)_/¯
                if (rv instanceof Mono<?> mono) {
                    return mono.block();
                } else {
                    throw new AssertionError("Unexpected Mono type");
                }
            } catch (UndeclaredThrowableException ex) {
                throwable = ex.getUndeclaredThrowable();
            } catch (DataAccessException ex) {
                throwable = ex;
            }

            Throwable cause = NestedExceptionUtils.getMostSpecificCause(throwable);
            if (cause instanceof PostgresqlException) {
                retryCause = ((PostgresqlException) cause).getErrorDetails();
                if (isRetryable(retryCause)) {
                    handleTransientException(retryCause, methodCalls, pjp.getSignature());
                } else {
                    handleNonTransientException(retryCause);
                    throw throwable;
                }
            } else {
                throw throwable;
            }
        } while (methodCalls - 1 < retryAttempts);

        throw new ConcurrencyFailureException(
                "Too many transient SQL errors (" + methodCalls + ") for method ["
                        + pjp.getSignature().toShortString()
                        + "]. Giving up!");
    }

    protected boolean isRetryable(ErrorDetails details) {
        return "40001".equals(details.getCode());
    }

    protected void handleNonTransientException(ErrorDetails details) {
        logger.warn("Non-transient SQL error ({}): {}", details.getDetail(), details.getMessage());
    }

    protected void handleTransientException(ErrorDetails details,
                                            int methodCalls,
                                            Signature signature) {
        try {
            long backoffMillis = Math.min((long) (Math.pow(2, methodCalls) + Math.random() * 1000), maxBackoff);
            if (logger.isWarnEnabled()) {
                logger.warn("Transient SQL error ({}) for method [{}] attempt ({}) backoff {} ms: {}",
                        details.getCode(),
                        signature.toShortString(),
                        methodCalls,
                        backoffMillis,
                        details.getMessage());
            }
            TimeUnit.MILLISECONDS.sleep(backoffMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void handleExceptionRecovery(ErrorDetails details,
                                           int methodCalls,
                                           Signature signature,
                                           Duration elapsedTime) {
        logger.debug("Recovered from transient SQL error ({}) for method [{}}] "
                        + "attempt ({}) time spent: {}",
                details.getCode(),
                signature.toShortString(),
                methodCalls,
                elapsedTime);
    }
}
