package io.roach.data.parallel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.UndeclaredThrowableException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyUtilsTest {
    private static final Logger logger = LoggerFactory.getLogger(ConcurrencyUtilsTest.class);

    private static <V> long executionTime(Callable<V> task) {
        try {
            long start = System.nanoTime();
            task.call();
            Duration nanos = Duration.ofNanos(System.nanoTime() - start);
            logger.debug("{} completed in {}", task, nanosToDisplayString(nanos));
            return nanos.toMillis();
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private static String nanosToDisplayString(Duration nanos) {
        return String.format(Locale.US, "%s", nanos.toString());
    }

    public static Integer doMassiveCompute_AndSucceed(int value, long minWait, long maxWait) {
        long wait = minWait + (long) (Math.random() * maxWait);

        logger.debug("Doing massive compute ({}) - will succeed after {}", value, wait);
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted!");
        }
        logger.debug("Done with compute {}", value);
        return value;
    }

    public static Integer doMassiveCompute_AndFail(int value, long minWait, long maxWait) {
        long wait = minWait + (long) (Math.random() * maxWait);

        logger.debug("Doing massive compute ({}) - will fail after {}", value, wait);
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted!");
        }
        throw new RuntimeException("Something went wrong (fake)");
    }

    @Test
    public void whenSchedulingManyTasksThatCompleteWithinTime_thenSucceed() {
        List<Callable<Integer>> tasks = new ArrayList<>();
        Random random = ThreadLocalRandom.current();

        List<Integer> numbers = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            int n = random.nextInt();
            numbers.add(n);
            tasks.add(() -> doMassiveCompute_AndSucceed(n, 100, 500));
        }

        long time = executionTime(() -> {
            ConcurrencyUtils.runConcurrentlyAndWait(tasks, 10, TimeUnit.SECONDS, result -> {
                logger.info("Result: {}", result);
                Assertions.assertTrue(numbers.remove(result));
            }, throwable -> {
                logger.error("Exception", throwable);
                return null;
            });
            return null;
        });

        Assertions.assertEquals(0, numbers.size());
        Assertions.assertTrue(time <= 11_000, "" + time);
    }

    @Test
    public void whenSchedulingManyTasksThatTimeOut_thenSucceed() {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int k = i;
            tasks.add(() -> doMassiveCompute_AndSucceed(k, 10_000, 15_000));
        }

        long time = executionTime(() -> {
            ConcurrencyUtils.runConcurrentlyAndWait(tasks, 10, TimeUnit.SECONDS, result -> {
                logger.info("Result: {}", result);
                Assertions.assertNull(result);
            }, throwable -> {
                logger.error("Exception", throwable);
                return null;
            });
            return null;
        });

        Assertions.assertTrue(time <= 11_000, "" + time);
    }

    @Test
    public void whenSchedulingManyTasksThatFail_thenSucceed() {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int k = i;
            tasks.add(() -> doMassiveCompute_AndFail(k, 100, 150));
        }

        AtomicInteger errors = new AtomicInteger();

        long time = executionTime(() -> {
            ConcurrencyUtils.runConcurrentlyAndWait(tasks, 10, TimeUnit.SECONDS, result -> {
                logger.info("Result: {}", result);
                Assertions.assertNull(result);
            }, throwable -> {
                logger.error("Exception", throwable);
                errors.incrementAndGet();
                return null;
            });
            return null;
        });

        Assertions.assertEquals(10, errors.get());
        Assertions.assertTrue(time <= 11_000, "" + time);
    }
}
