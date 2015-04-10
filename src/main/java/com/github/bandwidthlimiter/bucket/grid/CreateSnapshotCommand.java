package com.github.bandwidthlimiter.bucket.grid;

import com.github.bandwidthlimiter.bucket.BucketState;

public class CreateSnapshotCommand implements GridCommand<long[]> {

    @Override
    public long[] execute(GridBucketState gridState) {
        BucketState state = gridState.getBucketState();
        return state.createSnapshot();
    }

    @Override
    public boolean isBucketStateModified() {
        return false;
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

}
