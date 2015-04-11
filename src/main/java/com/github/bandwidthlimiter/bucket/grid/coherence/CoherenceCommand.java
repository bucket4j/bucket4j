package com.github.bandwidthlimiter.bucket.grid.coherence;

import com.github.bandwidthlimiter.bucket.grid.GridBucketState;
import com.github.bandwidthlimiter.bucket.grid.GridCommand;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.Serializable;

public class CoherenceCommand<T extends Serializable> extends AbstractProcessor {

    private final GridCommand<T> command;

    public CoherenceCommand(GridCommand<T> command) {
        this.command = command;
    }

    @Override
    public Object process(InvocableMap.Entry entry) {
        GridBucketState state = (GridBucketState) entry.getValue();
        T result = command.execute(state);
        if (command.isBucketStateModified()) {
            entry.setValue(state);
        }
        return result;
    }

}
