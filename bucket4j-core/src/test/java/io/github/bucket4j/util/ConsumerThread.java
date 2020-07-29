
package io.github.bucket4j.util;

import io.github.bucket4j.Bucket;

import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

public class ConsumerThread extends Thread {

    private final CountDownLatch startLatch;
    private final CountDownLatch endLatch;
    private final Bucket bucket;
    private final long workTimeNanos;
    private final Function<Bucket, Long> action;
    private long consumed;
    private Throwable exception;

    public ConsumerThread(CountDownLatch startLatch, CountDownLatch endLatch, Bucket bucket, long workTimeNanos, Function<Bucket, Long> action) {
        this.startLatch = startLatch;
        this.endLatch = endLatch;
        this.bucket = bucket;
        this.workTimeNanos = workTimeNanos;
        this.action = action;
    }

    @Override
    public void run() {
        try {
            startLatch.countDown();
            startLatch.await();
            long endNanoTime = System.nanoTime() + workTimeNanos;
            while(true) {
                if (System.nanoTime() >= endNanoTime) {
                    return;
                }
                consumed += action.apply(bucket);
            }
        } catch (Throwable e) {
            exception = e;
            e.printStackTrace();
        } finally {
            endLatch.countDown();
        }
    }

    public Throwable getException() {
        return exception;
    }

    public long getConsumed() {
        return consumed;
    }

}
