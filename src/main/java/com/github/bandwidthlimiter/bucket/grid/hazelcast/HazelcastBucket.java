package com.github.bandwidthlimiter.bucket.grid.hazelcast;

import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.grid.AbstractGridBucket;

public class HazelcastBucket extends AbstractGridBucket {

    public HazelcastBucket(BucketConfiguration configuration) {
        super(configuration);
    }

}
