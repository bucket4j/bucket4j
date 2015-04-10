package com.github.bandwidthlimiter.bucket;

import java.util.Arrays;

public class BucketState {

    protected final long[] state;

    public BucketState(BucketConfiguration configuration) {
        final RefillStrategy refillStrategy = configuration.getRefillStrategy();
        this.state = new long[configuration.getBandwidths().length + refillStrategy.sizeOfState(configuration)];
        long currentTime = configuration.getTimeMeter().currentTime();
        refillStrategy.setupInitialState(configuration, this, currentTime);
    }

    public BucketState(BucketState previousState) {
        this.state = Arrays.copyOf(previousState.state, previousState.state.length);
    }

    public BucketState(long[] snapshot) {
        this.state = snapshot;
    }

    public long[] createSnapshot() {
        return Arrays.copyOf(state, state.length);
    }

    public long getCurrentSize(Bandwidth bandwidth) {
        return state[bandwidth.getIndexInBucket()];
    }

    public void setCurrentSize(Bandwidth bandwidth, long size) {
        state[bandwidth.getIndexInBucket()] = size;
    }

    public long getRefillState(BucketConfiguration configuration, int offset) {
        return state[configuration.getBandwidthCount() + offset];
    }

    public void setRefillState(BucketConfiguration configuration, int offset, long value) {
        state[configuration.getBandwidthCount() + offset] = value;
    }

    @Override
    public BucketState clone() {
        return new BucketState(this);
    }

    public void copyState(BucketState state) {
        System.arraycopy(state.state, 0, this.state, 0, this.state.length);
    }

    public void copyState(long[] state) {
        System.arraycopy(state, 0, this.state, 0, this.state.length);
    }

    public long getAvailableTokens(BucketConfiguration configuration) {
        long availableByLimitation = Long.MAX_VALUE;
        long availableByGuarantee = 0;
        for (Bandwidth bandwidth : configuration.getBandwidths()) {
            if (bandwidth.isLimited()) {
                availableByLimitation = Math.min(availableByLimitation, getCurrentSize(bandwidth));
            } else {
                availableByGuarantee = getCurrentSize(bandwidth);
            }
        }
        return Math.max(availableByLimitation, availableByGuarantee);
    }

    public void consume(BucketConfiguration configuration, long toConsume) {
        final int bandwidthCount = configuration.getBandwidthCount();
        for (int i = 0; i < bandwidthCount; i++) {
            state[i] = Math.max(0, state[i] - toConsume);
        }
    }

    public long calculateTimeToCloseDeficit(BucketConfiguration configuration, long deficit) {
        Bandwidth[] bandwidths = configuration.getBandwidths();
        long sleepToRefillLimited = 0;
        long sleepToRefillGuaranteed = Long.MAX_VALUE;
        for (int i = 1; i < bandwidths.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            if (bandwidth.isLimited()) {
                sleepToRefillLimited = Math.max(sleepToRefillLimited, timeRequiredToRefill(configuration, bandwidth, deficit));
            } else {
                sleepToRefillGuaranteed = timeRequiredToRefill(configuration, bandwidth, deficit);
            }
        }
        return Math.min(sleepToRefillLimited, sleepToRefillGuaranteed);
    }

    public long timeRequiredToRefill(BucketConfiguration configuration, Bandwidth bandwidth, long numTokens) {
        return configuration.getRefillStrategy().timeRequiredToRefill(configuration, bandwidth, numTokens);
    }

}
