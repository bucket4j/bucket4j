package com.github.bandwidthlimiter.bucket.grid.coherence;

import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.grid.AbstractGridBucket;
import com.github.bandwidthlimiter.bucket.grid.GridCommand;
import com.tangosol.net.NamedCache;

import java.io.Serializable;

public class CoherenceBucket extends AbstractGridBucket {

    private final NamedCache cache;
    private final Object key;

    protected CoherenceBucket(BucketConfiguration configuration, NamedCache cache, Object key) {
        super(configuration);
        this.cache = cache;
        this.key = key;
    }

    @Override
    protected <T extends Serializable> T execute(GridCommand<T> command) {
        CoherenceCommand<T> entryProcessor = new CoherenceCommand<>(command);
        return (T) cache.invoke(key, entryProcessor);
    }

}
