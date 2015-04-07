package com.github.bandwidthlimiter.leakybucket;

public class GenericCellRateRefillStrategy implements RefillStrategy {

    public static final RefillStrategy INSTANCE = new GenericCellRateRefillStrategy();

    @Override
    public void setupInitialState(LeakyBucketConfiguration configuration, LeakyBucketState state, long currentTime) {
        Bandwidth[] bandwidths = configuration.getAllBandwidths();
        for (int i = 0; i < bandwidths.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            state.setCurrentSize(i, bandwidth.getInitialCapacity());
            state.setRefillMarker(i, currentTime);
        }
    }

    @Override
    public void refill(LeakyBucketConfiguration configuration, LeakyBucketState state, long currentTime) {
        Bandwidth[] bandwidths = configuration.getAllBandwidths();
        for (int i = 0; i < bandwidths.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            long previousRefillTime = state.getRefillMarker(i);
            final long maxCapacity = bandwidth.getMaxCapacity();
            long calculatedRefill = (currentTime - previousRefillTime) * maxCapacity / bandwidth.getPeriod();
            if (calculatedRefill > 0) {
                long newSize = state.getCurrentSize(i) + calculatedRefill;
                newSize = Math.min(maxCapacity, newSize);
                state.setCurrentSize(i, newSize);
                state.setRefillMarker(i, currentTime);
            }
        }
    }

    @Override
    public long timeRequiredToRefill(Bandwidth bandwidth, long previousRefillMarker, long currentTime, long numTokens) {
        return bandwidth.getPeriod() * numTokens / bandwidth.getMaxCapacity();
    }

}
