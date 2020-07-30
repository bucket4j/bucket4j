package io.github.bucket4j.distributed.proxy.optimization;

/**
 * TODO
 */
public interface OptimizationListener {

    static OptimizationListener createDefault() {
        return new DefaultOptimizationListener();
    }

    /**
     * TODO
     *
     * @param count
     */
    void incrementMergeCount(int count);

    /**
     * TODO
     *
     * @param count
     */
    void incrementSkipCount(int count);

}
