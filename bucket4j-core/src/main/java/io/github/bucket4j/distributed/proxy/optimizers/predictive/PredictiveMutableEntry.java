package io.github.bucket4j.distributed.proxy.optimizers.predictive;

import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

class PredictiveMutableEntry implements MutableBucketEntry {

    private final RemoteBucketState originalState;
    private RemoteBucketState newState;

    PredictiveMutableEntry(RemoteBucketState originalState) {
        this.originalState = originalState;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public void set(RemoteBucketState state) {
        this.newState = state;
    }

    @Override
    public RemoteBucketState get() {
        return originalState;
    }

    public RemoteBucketState getNewState() {
        return newState;
    }

}
