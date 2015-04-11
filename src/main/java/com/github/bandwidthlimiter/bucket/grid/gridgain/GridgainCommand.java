package com.github.bandwidthlimiter.bucket.grid.gridgain;

import com.github.bandwidthlimiter.bucket.grid.GridBucketState;
import com.github.bandwidthlimiter.bucket.grid.GridCommand;
import org.gridgain.grid.lang.GridBiTuple;
import org.gridgain.grid.lang.GridClosure;

import java.io.Serializable;

public class GridgainCommand<T extends Serializable> implements GridClosure<GridBucketState, GridBiTuple<GridBucketState, T>> {

    private final GridCommand<T> targetCommand;

    public GridgainCommand(GridCommand<T> targetCommand) {
        this.targetCommand = targetCommand;
    }

    @Override
    public GridBiTuple<GridBucketState, T> apply(GridBucketState gridBucketState) {
        T result = targetCommand.execute(gridBucketState);
        return new GridBiTuple<>(gridBucketState, result);
    }

}
