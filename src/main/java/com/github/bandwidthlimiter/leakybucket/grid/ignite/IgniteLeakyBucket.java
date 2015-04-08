package com.github.bandwidthlimiter.leakybucket.grid.ignite;

import com.github.bandwidthlimiter.leakybucket.LeakyBucketConfiguration;
import com.github.bandwidthlimiter.leakybucket.grid.AbstractGridLeakyBucket;

public class IgniteLeakyBucket extends AbstractGridLeakyBucket {

    public IgniteLeakyBucket(LeakyBucketConfiguration configuration) {
        super(configuration);
    }
}
