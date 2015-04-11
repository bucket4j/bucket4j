package com.github.bandwidthlimiter.bucket.grid.gridgain;

import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.BucketState;
import com.github.bandwidthlimiter.bucket.grid.AbstractGridBucket;
import com.github.bandwidthlimiter.bucket.grid.GridBucketState;
import com.github.bandwidthlimiter.bucket.grid.GridCommand;
import org.gridgain.grid.GridException;
import org.gridgain.grid.cache.GridCache;

import java.io.Serializable;

public class GridgainGridBucket extends AbstractGridBucket {

    private final GridCache<Object, GridBucketState> cache;
    private final Object key;

    public GridgainGridBucket(BucketConfiguration configuration, GridCache<Object, GridBucketState> cache, Object key) throws GridException {
        super(configuration);
        this.cache = cache;
        this.key = key;

        GridBucketState initial = new GridBucketState(configuration, new BucketState(configuration));
        cache.putIfAbsent(key, initial);
    }

    @Override
    protected <T extends Serializable> T execute(GridCommand<T> command) {
        try {
            return cache.transformAndCompute(key, new GridgainCommand<>(command));
        } catch (GridException e) {
            throw new RuntimeException(e);
        }
    }

}
