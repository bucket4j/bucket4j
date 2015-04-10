package com.github.bandwidthlimiter.bucket;

public class GenericCellRateRefillStrategy implements RefillStrategy {

    public static final RefillStrategy INSTANCE = new GenericCellRateRefillStrategy();

    @Override
    public void setupInitialState(BucketConfiguration configuration, BucketState state, long currentTime) {
        for (Bandwidth bandwidth: configuration.getBandwidths()) {
            state.setCurrentSize(bandwidth, bandwidth.getInitialCapacity());
            state.setRefillState(configuration, bandwidth.getIndexInBucket(), currentTime);
        }
    }

    @Override
    public void refill(BucketConfiguration configuration, BucketState state, long currentTime) {
        for (Bandwidth bandwidth: configuration.getBandwidths()) {
            final int i = bandwidth.getIndexInBucket();
            long previousRefillTime = state.getRefillState(configuration, i);
            final long maxCapacity = bandwidth.getMaxCapacity();
            long durationSinceLastRefill = currentTime - previousRefillTime;
            final long period = bandwidth.getPeriod();

            if (durationSinceLastRefill > period) {
                state.setCurrentSize(bandwidth, maxCapacity);
                state.setRefillState(configuration, i, currentTime);
                continue;
            }

            long calculatedRefill = maxCapacity * durationSinceLastRefill / period;
            if (calculatedRefill == 0) {
                continue;
            }

            long newSize = state.getCurrentSize(bandwidth) + calculatedRefill;
            if (newSize >= maxCapacity) {
                state.setCurrentSize(bandwidth, maxCapacity);
                state.setRefillState(configuration, i, currentTime);
                continue;
            }

            state.setCurrentSize(bandwidth, newSize);
            long effectiveDuration = calculatedRefill * period / maxCapacity;
            long roundingError = durationSinceLastRefill - effectiveDuration;
            long effectiveRefillTime = currentTime - roundingError;
            state.setRefillState(configuration, i, effectiveRefillTime);
        }
    }

    @Override
    public long timeRequiredToRefill(BucketConfiguration configuration, Bandwidth bandwidth, long numTokens) {
        return bandwidth.getPeriod() * numTokens / bandwidth.getMaxCapacity();
    }

    @Override
    public int sizeOfState(BucketConfiguration configuration) {
        return configuration.getBandwidthCount();
    }

}
