package com.github.bandwidthlimiter.bucket.grid.ignite;

import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.grid.AbstractGridBucket;
import com.github.bandwidthlimiter.bucket.grid.GridBucketState;
import com.github.bandwidthlimiter.bucket.grid.GridCommand;
import org.apache.ignite.IgniteCache;

import java.io.Serializable;

public class IgniteBucket extends AbstractGridBucket {

    private final IgniteCache<Object, GridBucketState> cache;
    private final Object key;

    public IgniteBucket(BucketConfiguration configuration, IgniteCache<Object, GridBucketState> cache, Object key) {
        super(configuration);
        this.cache = cache;
        this.key = key;

        GridBucketState initial = new GridBucketState();
        cache.putIfAbsent(key, initial);
    }

    @Override
    protected <T extends Serializable> T execute(GridCommand<T> command) {
        return cache.invoke(key, new IgniteCommand<T>(), command);
    }

}
