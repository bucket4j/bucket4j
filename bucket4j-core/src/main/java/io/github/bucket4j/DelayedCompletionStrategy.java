package io.github.bucket4j;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public interface DelayedCompletionStrategy {

    <T> void completeWithDelay(CompletableFuture<T> future, T result, long delay, TimeUnit delayUnit);

    static DelayedCompletionStrategy forScheduler(ScheduledExecutorService scheduler) {
        return new DelayedCompletionStrategy() {
            @Override
            public <T> void completeWithDelay(CompletableFuture<T> future, T result, long delay, TimeUnit delayUnit) {
                Runnable task = () -> future.complete(result);
                scheduler.schedule(task, delay, delayUnit);
            }
        };
    }

}
