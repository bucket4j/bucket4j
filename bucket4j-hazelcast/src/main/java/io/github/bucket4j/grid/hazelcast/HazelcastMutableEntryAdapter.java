package io.github.bucket4j.grid.hazelcast;

import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

import java.util.Map;

class HazelcastMutableEntryAdapter<K> implements MutableBucketEntry {

    private final Map.Entry<K, RemoteBucketState> entry;
    private boolean modified;

    public HazelcastMutableEntryAdapter(Map.Entry<K, RemoteBucketState> entry) {
        this.entry = entry;
    }

    @Override
    public boolean exists() {
        return entry.getValue() != null;
    }

    @Override
    public void set(RemoteBucketState value) {
        entry.setValue(value);
        this.modified = true;
    }

    @Override
    public RemoteBucketState get() {
        return entry.getValue();
    }

    public boolean isModified() {
        return modified;
    }

}
