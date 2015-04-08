package com.github.bandwidthlimiter.leakybucket.grid.hazelcast;

import com.github.bandwidthlimiter.leakybucket.LeakyBucketConfiguration;
import com.github.bandwidthlimiter.leakybucket.grid.AbstractGridLeakyBucket;

public class HazelcastLeakyBucket extends AbstractGridLeakyBucket {

    public HazelcastLeakyBucket(LeakyBucketConfiguration configuration) {
        super(configuration);
    }

}
