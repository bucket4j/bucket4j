package com.github.bandwidthlimiter.leakybucket.grid;

import com.github.bandwidthlimiter.leakybucket.AbstractLeakyBucket;
import com.github.bandwidthlimiter.leakybucket.LeakyBucketConfiguration;
import com.github.bandwidthlimiter.leakybucket.LeakyBucketState;

public class AbstractGridLeakyBucket extends AbstractLeakyBucket {

    protected AbstractGridLeakyBucket(LeakyBucketConfiguration configuration) {
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
    public LeakyBucketState createSnapshot() {
        return null;
    }
}
