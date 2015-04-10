package com.github.bandwidthlimiter.bucket.grid.ignite;

import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.grid.AbstractGridBucket;
import com.github.bandwidthlimiter.bucket.grid.GridCommand;

import java.io.Serializable;

public class IgniteBucket extends AbstractGridBucket {

    public IgniteBucket(BucketConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected <T extends Serializable> T execute(GridCommand<T> command) {
        return null;
    }
}
