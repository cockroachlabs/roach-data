package io.roach.data.parallel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class ConcurrencyUtils {
    private static final Logger logger = LoggerFactory.getLogger(ConcurrencyUtils.class);

    private ConcurrencyUtils() {
    }

    private static ExecutorService boundedThreadPool() {
        int numThreads = Runtime.getRuntime().availableProcessors() * 4;

        ThreadPoolExecutor executor = new ThreadPoolExecutor(numThreads / 2, numThreads,
                0L, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(numThreads));
        executor.setRejectedExecutionHandler((runnable, exec) -> {
            try {
                exec.getQueue().put(runnable);
                if (exec.isShutdown()) {
                    throw new RejectedExecutionException(
                            "Task " + runnable + " rejected from " + exec);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("", e);
            }
        });
        return executor;
    }

    private static ExecutorService unboundedThreadPool() {
        return Executors.newCachedThreadPool();
    }

    public static <V> void runConcurrentlyAndWait(List<Callable<V>> tasks,
                                                  long timeout,
                                                  TimeUnit timeUnit,
                                                  Consumer<V> completion,
                                                  Function<Throwable, ? extends Void> failure) {
        final ScheduledExecutorService cancellationService
                = Executors.newSingleThreadScheduledExecutor();

        final ExecutorService executor = boundedThreadPool();

        List<CompletableFuture<Void>> allFutures = new ArrayList<>();

        final Instant expiryTime = Instant.now().plusMillis(timeUnit.toMillis(timeout));

        tasks.forEach(callable -> {
            CompletableFuture<Void> f = CompletableFuture.supplyAsync(() -> {
                        if (Instant.now().isAfter(expiryTime)) {
                            logger.warn("Task scheduled after expiration time: " + expiryTime);
                            return null;
                        }
                        Future<V> future = executor.submit(callable);
                        long cancellationTime = Duration.between(Instant.now(), expiryTime).toMillis();
                        cancellationService.schedule(() -> future.cancel(true), cancellationTime, TimeUnit.MILLISECONDS);
                        try {
                            return future.get();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e.getCause());
                        } catch (ExecutionException e) {
                            throw new IllegalStateException(e.getCause());
                        }
                    })
                    .thenAccept(completion)
                    .exceptionally(failure);
            allFutures.add(f);
        });

        CompletableFuture.allOf(
                allFutures.toArray(new CompletableFuture[]{})).join();

        executor.shutdownNow();
        cancellationService.shutdownNow();
    }

    public static <V> void runConcurrentlyAndWait(List<Callable<V>> tasks,
                                                  Consumer<V> completion,
                                                  Function<Throwable, ? extends Void> failure) {
        ExecutorService executor = boundedThreadPool();

        List<CompletableFuture<Void>> allFutures = new ArrayList<>();

        tasks.forEach(callable -> {
            CompletableFuture<Void> f = CompletableFuture.supplyAsync(() -> {
                        try {
                            return callable.call();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e.getCause());
                        } catch (Exception e) {
                            throw new IllegalStateException(e.getCause());
                        }
                    }, executor)
                    .thenAccept(completion)
                    .exceptionally(failure);
            allFutures.add(f);
        });

        CompletableFuture.allOf(
                allFutures.toArray(new CompletableFuture[]{})).join();

        executor.shutdown();
    }
}
