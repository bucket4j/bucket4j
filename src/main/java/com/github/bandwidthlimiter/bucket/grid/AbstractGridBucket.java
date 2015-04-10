package com.github.bandwidthlimiter.bucket.grid;

import com.github.bandwidthlimiter.bucket.AbstractBucket;
import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.BucketState;

public class AbstractGridBucket extends AbstractBucket {

    protected AbstractGridBucket(BucketConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        return 0;
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        return false;
    }

    @Override
    protected boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyNanos) throws InterruptedException {
        return false;
    }

    @Override
    public BucketState createSnapshot() {
        return null;
    }
}
