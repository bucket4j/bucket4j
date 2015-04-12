package com.github.bandwidthlimiter.bucket.grid.ignite;

import com.github.bandwidthlimiter.bucket.grid.GridBucketState;
import com.github.bandwidthlimiter.bucket.grid.GridCommand;
import com.github.bandwidthlimiter.bucket.grid.GridProxy;
import org.apache.ignite.IgniteCache;

import java.io.Serializable;

public class IgniteProxy implements GridProxy {

    private final IgniteCache<Object, GridBucketState> cache;
    private final Object key;

    public IgniteProxy(IgniteCache<Object, GridBucketState> cache, Object key) {
        this.cache = cache;
        this.key = key;
    }

    @Override
    public <T extends Serializable> T execute(GridCommand<T> command) {
        return cache.invoke(key, new IgniteCommand<T>(), command);
    }

    @Override
    public void setInitialState(GridBucketState initialState) {
        cache.putIfAbsent(key, initialState);
    }

}
