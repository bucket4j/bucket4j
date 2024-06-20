package io.github.bucket4j.distributed.proxy.synchronization;

import io.github.bucket4j.distributed.proxy.optimization.OptimizationListener;

public class OptimizationListenerAdapter implements OptimizationListener {

    private final SynchronizationListener target;

    public OptimizationListenerAdapter(SynchronizationListener target) {
        this.target = target;
    }

    @Override
    public void incrementMergeCount(int count) {
        target.incrementMergeCount(count);
    }

    @Override
    public void incrementSkipCount(int count) {
        target.incrementSkipCount(count);
    }

}
