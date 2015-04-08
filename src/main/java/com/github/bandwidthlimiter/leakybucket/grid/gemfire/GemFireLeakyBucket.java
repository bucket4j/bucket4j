package com.github.bandwidthlimiter.leakybucket.grid.gemfire;

import com.github.bandwidthlimiter.leakybucket.LeakyBucketConfiguration;
import com.github.bandwidthlimiter.leakybucket.grid.AbstractGridLeakyBucket;

public class GemFireLeakyBucket extends AbstractGridLeakyBucket {

    public GemFireLeakyBucket(LeakyBucketConfiguration configuration) {
        super(configuration);
    }
}
