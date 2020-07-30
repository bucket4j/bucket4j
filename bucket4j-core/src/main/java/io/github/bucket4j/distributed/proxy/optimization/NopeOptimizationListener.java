package io.github.bucket4j.distributed.proxy.optimization;

public class NopeOptimizationListener implements OptimizationListener {

    public static final NopeOptimizationListener INSTANCE = new NopeOptimizationListener();

    @Override
    public void incrementMergeCount(int count) {
        // do nothing
    }

    @Override
    public void incrementSkipCount(int count) {
        // do nothing
    }

}
