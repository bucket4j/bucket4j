package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.map.EntryBackupProcessor;
import io.github.bucket4j.grid.GridBucketState;

import java.util.Map;


class SimpleBackupProcessor implements EntryBackupProcessor {

    private final GridBucketState state;

    public SimpleBackupProcessor(GridBucketState state) {
        this.state = state;
    }

    @Override
    public void processBackup(Map.Entry entry) {
        entry.setValue(state);
    }

}
