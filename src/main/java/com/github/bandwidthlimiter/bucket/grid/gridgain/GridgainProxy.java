package com.github.bandwidthlimiter.bucket.grid.gridgain;

import com.github.bandwidthlimiter.bucket.grid.GridBucketState;
import com.github.bandwidthlimiter.bucket.grid.GridCommand;
import com.github.bandwidthlimiter.bucket.grid.GridProxy;
import org.gridgain.grid.GridException;
import org.gridgain.grid.cache.GridCache;

import java.io.Serializable;

public class GridgainProxy implements GridProxy {

    private final GridCache<Object, GridBucketState> cache;
    private final Object key;

    public GridgainProxy(GridCache<Object, GridBucketState> cache, Object key) {
        this.cache = cache;
        this.key = key;
    }

    @Override
    public <T extends Serializable> T execute(GridCommand<T> command) {
        try {
            return cache.transformAndCompute(key, new GridgainCommand<>(command));
        } catch (GridException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setInitialState(GridBucketState initialState) {
        try {
            cache.putIfAbsent(key, initialState);
        } catch (GridException e) {
            throw new RuntimeException(e);
        }
    }

}
