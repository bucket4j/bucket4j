package com.github.bandwidthlimiter.leakybucket.grid.coherence;

import com.github.bandwidthlimiter.leakybucket.LeakyBucketConfiguration;
import com.github.bandwidthlimiter.leakybucket.grid.AbstractGridLeakyBucket;

public class CoherenceLeakyBucket extends AbstractGridLeakyBucket {

    protected CoherenceLeakyBucket(LeakyBucketConfiguration configuration) {
        super(configuration);
    }
}
