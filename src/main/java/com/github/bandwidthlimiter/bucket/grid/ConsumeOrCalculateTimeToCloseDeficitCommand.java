package com.github.bandwidthlimiter.bucket.grid;

import com.github.bandwidthlimiter.bucket.Bandwidth;
import com.github.bandwidthlimiter.bucket.BandwidthAlgorithms;
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
        Bandwidth[] bandwidths = configuration.getBandwidths();
        BandwidthAlgorithms.refill(bandwidths, state, currentTime);
        long timeToCloseDeficit = BandwidthAlgorithms.delayAfterWillBePossibleToConsume(bandwidths, state, currentTime, tokensToConsume);
        if (timeToCloseDeficit == 0) {
            BandwidthAlgorithms.consume(bandwidths, state, tokensToConsume);
            bucketStateModified = true;
        }
        return timeToCloseDeficit;
    }

    @Override
    public boolean isBucketStateModified() {
        return bucketStateModified;
    }

}
