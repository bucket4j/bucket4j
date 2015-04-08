package com.github.bandwidthlimiter.leakybucket;

public class GenericCellRateRefillStrategy implements RefillStrategy {

    public static final RefillStrategy INSTANCE = new GenericCellRateRefillStrategy();

    @Override
    public void setupInitialState(LeakyBucketConfiguration configuration, LeakyBucketState state, long currentTime) {
        for (Bandwidth bandwidth: configuration.getBandwidths()) {
            final int i = bandwidth.getIndexInBucket();
            state.setCurrentSize(i, bandwidth.getInitialCapacity());
            state.setRefillState(configuration, i, currentTime);
        }
    }

    @Override
    public void refill(LeakyBucketConfiguration configuration, LeakyBucketState state, long currentTime) {
        for (Bandwidth bandwidth: configuration.getBandwidths()) {
            final int i = bandwidth.getIndexInBucket();
            long previousRefillTime = state.getRefillState(configuration, i);
            final long maxCapacity = bandwidth.getMaxCapacity();
            long calculatedRefill = (currentTime - previousRefillTime) * maxCapacity / bandwidth.getPeriod();
            if (calculatedRefill > 0) {
                long newSize = state.getCurrentSize(i) + calculatedRefill;
                newSize = Math.min(maxCapacity, newSize);
                state.setCurrentSize(i, newSize);
                state.setRefillState(configuration, i, currentTime);
            }
        }
    }

    @Override
    public long timeRequiredToRefill(LeakyBucketConfiguration configuration, Bandwidth bandwidth, long numTokens) {
        return bandwidth.getPeriod() * numTokens / bandwidth.getMaxCapacity();
    }

    @Override
    public int sizeOfState(LeakyBucketConfiguration configuration) {
        return configuration.getBandwidthCount();
    }

}
