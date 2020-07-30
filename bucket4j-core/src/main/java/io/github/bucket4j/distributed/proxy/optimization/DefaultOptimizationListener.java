package io.github.bucket4j.distributed.proxy.optimization;

import java.util.concurrent.atomic.AtomicLong;

public class DefaultOptimizationListener implements OptimizationListener {

    private final AtomicLong mergeCount = new AtomicLong();
    private final AtomicLong skipCount = new AtomicLong();

    @Override
    public void incrementMergeCount(int count) {
        mergeCount.addAndGet(count);
    }

    @Override
    public void incrementSkipCount(int count) {
        skipCount.addAndGet(count);
    }

    public long getMergeCount() {
        return mergeCount.get();
    }

    public long getSkipCount() {
        return skipCount.get();
    }

}
