package com.github.bandwidthlimiter.bucket.grid.hazelcast;

import com.github.bandwidthlimiter.bucket.grid.GridBucketState;
import com.github.bandwidthlimiter.bucket.grid.GridCommand;
import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;

import java.io.Serializable;
import java.util.Map;

public class HazelcastCommand<T extends Serializable> implements EntryProcessor<Object, GridBucketState>, Serializable {

    private final GridCommand<T> targetCommand;
    private long[] snapshotToBackup;

    public HazelcastCommand(GridCommand<T> targetCommand) {
        this.targetCommand = targetCommand;
    }

    @Override
    public T process(Map.Entry<Object, GridBucketState> entry) {
        GridBucketState gridState = entry.getValue();
        T result = targetCommand.execute(gridState);
        if (targetCommand.isBucketStateModified()) {
            entry.setValue(gridState);
            snapshotToBackup = gridState.getBucketState().createSnapshot();
        }
        return result;
    }

    @Override
    public EntryBackupProcessor getBackupProcessor() {
        if (snapshotToBackup == null) {
            return null;
        }
        return new HazelcastReplicant(snapshotToBackup);
    }
}
