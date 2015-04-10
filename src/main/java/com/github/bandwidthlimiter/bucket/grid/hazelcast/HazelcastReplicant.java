package com.github.bandwidthlimiter.bucket.grid.hazelcast;

import com.github.bandwidthlimiter.bucket.grid.GridBucketState;
import com.hazelcast.map.EntryBackupProcessor;

import java.io.Serializable;
import java.util.Map;

public class HazelcastReplicant implements EntryBackupProcessor, Serializable {

    private long[] snapshotToBackup;

    public HazelcastReplicant(long[] snapshotToBackup) {
        this.snapshotToBackup = snapshotToBackup;
    }

    public HazelcastReplicant() {
        return;
    }

    @Override
    public void processBackup(Map.Entry entry) {
        GridBucketState gridState = (GridBucketState) entry.getValue();
        gridState.getBucketState().copyState(snapshotToBackup);
        entry.setValue(gridState);
    }
}
