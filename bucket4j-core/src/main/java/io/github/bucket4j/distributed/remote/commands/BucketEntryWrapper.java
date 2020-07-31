package io.github.bucket4j.distributed.remote.commands;

import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

import java.util.Objects;

public class BucketEntryWrapper implements MutableBucketEntry {

    private RemoteBucketState state;
    private boolean stateModified;

    public BucketEntryWrapper(RemoteBucketState state) {
        this.state = state;
    }

    @Override
    public boolean exists() {
        return state != null;
    }

    public boolean isStateModified() {
        return stateModified;
    }

    @Override
    public void set(RemoteBucketState state) {
        this.state = Objects.requireNonNull(state);
        this.stateModified = true;
    }

    @Override
    public RemoteBucketState get() {
        if (state == null) {
            throw new IllegalStateException("'exists' must be called before 'get'");
        }
        return state;
    }

}
