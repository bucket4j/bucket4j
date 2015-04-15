package com.github.bandwidthlimiter.bucket.grid;

import com.github.bandwidthlimiter.bucket.Bandwidth;
import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.BucketState;

public class TryConsumeCommand implements GridCommand<Boolean> {

    private long tokensToConsume;
    private boolean bucketStateModified;

    public TryConsumeCommand(long tokensToConsume) {
        this.tokensToConsume = tokensToConsume;
    }

    @Override
    public Boolean execute(GridBucketState gridState) {
        BucketConfiguration configuration = gridState.getBucketConfiguration();
        BucketState state = gridState.getBucketState();
        long currentTime = configuration.getTimeMeter().currentTime();
        Bandwidth[] bandwidths = configuration.getBandwidths();
        state.refill(bandwidths, currentTime);
        long availableToConsume = state.getAvailableTokens(bandwidths);
        if (tokensToConsume <= availableToConsume) {
            state.consume(bandwidths, tokensToConsume);
            bucketStateModified = true;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isBucketStateModified() {
        return bucketStateModified;
    }

}
