package com.github.bandwidthlimiter.bucket.grid.ignite;

import com.github.bandwidthlimiter.bucket.grid.GridBucketState;
import com.github.bandwidthlimiter.bucket.grid.GridCommand;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.io.Serializable;

public class IgniteCommand<T extends Serializable> implements EntryProcessor<Object, GridBucketState, T> {

    @Override
    public T process(MutableEntry<Object, GridBucketState> mutableEntry, Object... arguments) throws EntryProcessorException {
        GridCommand<T> targetCommand = (GridCommand<T>) arguments[0];
        GridBucketState state = mutableEntry.getValue();
        T result = targetCommand.execute(state);
        if (targetCommand.isBucketStateModified()) {
            mutableEntry.setValue(state);
        }
        return result;
    }

}
