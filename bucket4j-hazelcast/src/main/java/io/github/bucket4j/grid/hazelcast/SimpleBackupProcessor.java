package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.map.EntryBackupProcessor;
import io.github.bucket4j.grid.GridBucketState;

import java.io.Serializable;
import java.util.Map;


class SimpleBackupProcessor<K extends Serializable> implements EntryBackupProcessor<K, GridBucketState> {

    private static final long serialVersionUID = 1L;

    private final GridBucketState state;

    public SimpleBackupProcessor(GridBucketState state) {
        this.state = state;
    }

    @Override
    public void processBackup(Map.Entry<K, GridBucketState> entry) {
        entry.setValue(state);
    }

}
