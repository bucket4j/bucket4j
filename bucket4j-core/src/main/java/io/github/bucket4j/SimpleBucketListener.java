
package io.github.bucket4j;

import java.util.concurrent.atomic.AtomicLong;

public class SimpleBucketListener implements BucketListener {

    private AtomicLong consumed = new AtomicLong();
    private AtomicLong rejected = new AtomicLong();
    private AtomicLong delayedNanos = new AtomicLong();
    private AtomicLong parkedNanos = new AtomicLong();
    private AtomicLong interrupted = new AtomicLong();

    @Override
    public void onConsumed(long tokens) {
        consumed.addAndGet(tokens);
    }

    @Override
    public void onRejected(long tokens) {
        rejected.addAndGet(tokens);
    }

    @Override
    public void onDelayed(long nanos) {
        delayedNanos.addAndGet(nanos);
    }

    @Override
    public void onParked(long nanos) {
        parkedNanos.addAndGet(nanos);
    }

    @Override
    public void onInterrupted(InterruptedException e) {
        interrupted.incrementAndGet();
    }

    public long getConsumed() {
        return consumed.get();
    }

    public long getRejected() {
        return rejected.get();
    }

    public long getDelayedNanos() {
        return delayedNanos.get();
    }

    public long getParkedNanos() {
        return parkedNanos.get();
    }

    public long getInterrupted() {
        return interrupted.get();
    }

}
