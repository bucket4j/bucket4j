package io.github.bucket4j.grid.hazelcast;

import io.github.bucket4j.grid.GridBucketState;

import javax.cache.processor.MutableEntry;
import java.io.Serializable;
import java.util.Map;


class HazelcastMutableEntryAdapter<K extends Serializable> implements MutableEntry<K, GridBucketState> {

    private final Map.Entry<K, GridBucketState> entry;

    public HazelcastMutableEntryAdapter(Map.Entry<K, GridBucketState> entry) {
        this.entry = entry;
    }

    @Override
    public boolean exists() {
        return entry.getValue() != null;
    }

    @Override
    public void remove() {
        entry.setValue(null);
    }

    @Override
    public void setValue(GridBucketState value) {
        entry.setValue(value);
    }

    @Override
    public K getKey() {
        return entry.getKey();
    }

    @Override
    public GridBucketState getValue() {
        return entry.getValue();
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        throw new UnsupportedOperationException();
    }
}
