package io.github.bucket4j.distributed.proxy.optimization;

import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

public class InMemoryMutableEntry implements MutableBucketEntry {

    private final RemoteBucketState originalState;
    private RemoteBucketState newState;

    public InMemoryMutableEntry(RemoteBucketState originalState) {
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
        return newState != null? newState : originalState;
    }

    public RemoteBucketState getNewState() {
        return newState;
    }

}
