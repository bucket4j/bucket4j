package com.github.bandwidthlimiter.bucket.grid;

import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.BucketState;

public class ConsumeAsMuchAsPossibleCommand implements GridCommand<Long> {

    private long limit;
    private boolean bucketStateModified;

    public ConsumeAsMuchAsPossibleCommand(long limit) {
        this.limit = limit;
    }

    @Override
    public Long execute(GridBucketState gridState) {
        BucketConfiguration configuration = gridState.getBucketConfiguration();
        BucketState state = gridState.getBucketState();
        long currentTime = configuration.getTimeMeter().currentTime();
        configuration.getRefillStrategy().refill(configuration, state, currentTime);
        long availableToConsume = state.getAvailableTokens(configuration);
        long toConsume = Math.min(limit, availableToConsume);
        if (toConsume <= 0) {
            return 0l;
        }
        state.consume(configuration, toConsume);
        bucketStateModified = true;
        return toConsume;
    }

    @Override
    public boolean isBucketStateModified() {
        return bucketStateModified;
    }

}
