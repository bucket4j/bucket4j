package com.github.bandwidthlimiter.bucket.grid.coherence;

import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.grid.AbstractGridBucket;
import com.github.bandwidthlimiter.bucket.grid.GridCommand;

import java.io.Serializable;

public class CoherenceBucket extends AbstractGridBucket {

    protected CoherenceBucket(BucketConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected <T extends Serializable> T execute(GridCommand<T> command) {
        return null;
    }
}
