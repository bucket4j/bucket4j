package com.github.bandwidthlimiter.leakybucket;

import com.github.bandwidthlimiter.leakybucket.Bandwidth;
import com.github.bandwidthlimiter.leakybucket.LeakyBucketConfiguration;
import com.github.bandwidthlimiter.leakybucket.LeakyBucketState;
import com.github.bandwidthlimiter.leakybucket.RefillStrategy;

public class TokenBucketRefillStrategy implements RefillStrategy {

    public TokenBucketRefillStrategy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setupInitialState(LeakyBucketConfiguration configuration, LeakyBucketState state, long currentTime) {
//        Bandwidth[] bandwidths = configuration.getAllBandwidths();
//        for (int i = 0; i < bandwidths.length; i++) {
//            Bandwidth bandwidth = bandwidths[i];
//            state.setCurrentSize(i, bandwidth.getInitialCapacity());
//            state.setRefillState(i, currentTime);
//        }
    }

    @Override
    public void refill(LeakyBucketConfiguration configuration, LeakyBucketState state, long currentTime) {
//        Bandwidth[] bandwidths = configuration.getAllBandwidths();
//        for (int i = 0; i < bandwidths.length; i++) {
//            Bandwidth bandwidth = bandwidths[i];
//            long previousRefillTime = state.getRefillState(i);
//            final long maxCapacity = bandwidth.getMaxCapacity();
//            long calculatedRefill = (currentTime - previousRefillTime) * maxCapacity / bandwidth.getPeriod();
//            if (calculatedRefill > 0) {
//                long newSize = state.getCurrentSize(i) + calculatedRefill;
//                newSize = Math.min(maxCapacity, newSize);
//                state.setCurrentSize(i, newSize);
//                state.setRefillState(i, currentTime);
//            }
//        }
    }

    @Override
    public long timeRequiredToRefill(Bandwidth bandwidth, long previousRefillMarker, long currentTime, long numTokens) {
        return bandwidth.getPeriod() * numTokens / bandwidth.getMaxCapacity();
    }

}
