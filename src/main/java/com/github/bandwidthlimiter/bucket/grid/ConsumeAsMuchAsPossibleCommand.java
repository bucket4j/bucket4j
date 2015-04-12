package com.github.bandwidthlimiter.bucket.grid;

import com.github.bandwidthlimiter.bucket.Bandwidth;
import com.github.bandwidthlimiter.bucket.BandwidthAlgorithms;
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
        Bandwidth[] bandwidths = configuration.getBandwidths();
        BandwidthAlgorithms.refill(bandwidths, state, currentTime);
        long availableToConsume = BandwidthAlgorithms.getAvailableTokens(bandwidths, state);
        long toConsume = Math.min(limit, availableToConsume);
        if (toConsume <= 0) {
            return 0l;
        }
        BandwidthAlgorithms.consume(bandwidths, state, toConsume);
        bucketStateModified = true;
        return toConsume;
    }

    @Override
    public boolean isBucketStateModified() {
        return bucketStateModified;
    }

}
