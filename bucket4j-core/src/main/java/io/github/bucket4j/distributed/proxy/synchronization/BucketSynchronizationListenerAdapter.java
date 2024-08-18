package io.github.bucket4j.distributed.proxy.synchronization;

import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronizationListener;

public class BucketSynchronizationListenerAdapter implements BucketSynchronizationListener {

    private final SynchronizationListener target;

    public BucketSynchronizationListenerAdapter(SynchronizationListener target) {
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
