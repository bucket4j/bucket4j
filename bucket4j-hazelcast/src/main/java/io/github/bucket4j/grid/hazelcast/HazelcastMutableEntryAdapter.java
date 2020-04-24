package io.github.bucket4j.grid.hazelcast;

import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.serialization.InternalSerializationHelper;

import java.util.Map;

import static io.github.bucket4j.serialization.InternalSerializationHelper.deserializeState;

class HazelcastMutableEntryAdapter<K> implements MutableBucketEntry {

    private final Map.Entry<K, byte[]> entry;
    private boolean modified;

    public HazelcastMutableEntryAdapter(Map.Entry<K, byte[]> entry) {
        this.entry = entry;
    }

    @Override
    public boolean exists() {
        return entry.getValue() != null;
    }

    @Override
    public void set(RemoteBucketState value) {
        byte[] stateBytes = InternalSerializationHelper.serializeState(value);
        entry.setValue(stateBytes);
        this.modified = true;
    }

    @Override
    public RemoteBucketState get() {
        byte[] stateBytes = entry.getValue();
        return deserializeState(stateBytes);
    }

    public boolean isModified() {
        return modified;
    }

}
