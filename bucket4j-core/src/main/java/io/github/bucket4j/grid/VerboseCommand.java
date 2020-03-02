package io.github.bucket4j.grid;

import io.github.bucket4j.VerboseResult;

import java.io.Serializable;

public class VerboseCommand<T extends Serializable> implements GridCommand<VerboseResult<T>> {

    private final GridCommand<T> targetCommand;

    public VerboseCommand(GridCommand<T> targetCommand) {
        this.targetCommand = targetCommand;
    }

    @Override
    public VerboseResult<T> execute(GridBucketState state, long currentTimeNanos) {
        T result = targetCommand.execute(state, currentTimeNanos);
        return new VerboseResult<T>(currentTimeNanos, result, state.getConfiguration(), state.getState());
    }

    @Override
    public boolean isBucketStateModified() {
        return targetCommand.isBucketStateModified();
    }

}
