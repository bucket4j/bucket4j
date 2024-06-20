package io.github.bucket4j.distributed.proxy.synchronization;

public interface SynchronizationListener {

    /**
     * This method is invoked every time when several independent requests to the same bucket combined into a single one.
     *
     * @param count number of requests that were merged
     */
    void incrementMergeCount(int count);

    /**
     * This method is invoked every time when several requests were not propagated to external storage because optimization had decided that they can be served locally.
     *
     * @param count number of requests that were served locally without synchronization with external storage
     */
    void incrementSkipCount(int count);

}
