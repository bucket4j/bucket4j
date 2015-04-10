package com.github.bandwidthlimiter.bucket.grid;

import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.BucketState;

public class ConsumeOrCalculateTimeToCloseDeficitCommand implements GridCommand<Long> {

    private long tokensToConsume;
    private boolean bucketStateModified;

    public ConsumeOrCalculateTimeToCloseDeficitCommand(long tokensToConsume) {
        this.tokensToConsume = tokensToConsume;
    }

    @Override
    public Long execute(GridBucketState gridState) {
        BucketConfiguration configuration = gridState.getBucketConfiguration();
        BucketState state = gridState.getBucketState();
        long currentTime = configuration.getTimeMeter().currentTime();
        configuration.getRefillStrategy().refill(configuration, state, currentTime);
        long availableToConsume = state.getAvailableTokens(configuration);
        if (tokensToConsume <= availableToConsume) {
            state.consume(configuration, tokensToConsume);
            bucketStateModified = true;
            return 0l;
        } else {
            long deficitTokens = tokensToConsume - availableToConsume;
            long timeToCloseDeficit = state.calculateTimeToCloseDeficit(configuration, deficitTokens);
            return timeToCloseDeficit;
        }
    }

    @Override
    public boolean isBucketStateModified() {
        return bucketStateModified;
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

}
