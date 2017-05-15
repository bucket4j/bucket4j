package io.github.bucket4j.grid;


import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;

public class GetAvailableTokensCommand implements GridCommand<Long> {

    @Override
    public Long execute(GridBucketState gridState) {
        BucketConfiguration configuration = gridState.getBucketConfiguration();
        BucketState state = gridState.getBucketState();
        Bandwidth[] bandwidths = configuration.getBandwidths();
        long currentTimeNanos = configuration.getTimeMeter().currentTimeNanos();

        state.refillAllBandwidth(bandwidths, currentTimeNanos);
        return state.getAvailableTokens(bandwidths);
    }

    @Override
    public boolean isBucketStateModified() {
        return false;
    }

}
