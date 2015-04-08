package com.github.bandwidthlimiter.leakybucket.local;

import com.github.bandwidthlimiter.leakybucket.Bandwidth;
import com.github.bandwidthlimiter.leakybucket.LeakyBucketConfiguration;
import com.github.bandwidthlimiter.leakybucket.GenericCellConfiguration;
import com.github.bandwidthlimiter.leakybucket.LeakyBucketState;

import java.util.Arrays;

public class LeakyBucketLocalState implements LeakyBucketState {

    public static final int SIZE_OFFSET = 0;
    public static final int MARKER_OFFSET = 1;

    private final long[] state;

    @Override
    public long getRefillMarker(int bandwidthIndex) {
        return state[bandwidthIndex * 2 + MARKER_OFFSET];
    }

    @Override
    public void setRefillMarker(int bandwidthIndex, long refillMarker) {
        state[bandwidthIndex * 2 + MARKER_OFFSET] = refillMarker;
    }

    @Override
    public long getCurrentSize(int bandwidthIndex) {
        return 0;
    }

    @Override
    public void setCurrentSize(int bandwidthIndex, long size) {

    }

    LeakyBucketLocalState(LeakyBucketConfiguration configuration) {
        this.state = new long[configuration.getAllBandwidths().length * 2];
        configuration.getRefillStrategy().setupInitialState(configuration, this);
    }

    private LeakyBucketLocalState() {}

    public LeakyBucketLocalState clone() {
        return new LeakyBucketLocalState(this);
    }

    private LeakyBucketLocalState(LeakyBucketLocalState previousState) {
        this.state = Arrays.copyOf(previousState.state, previousState.state.length);
    }

    void copyState(LeakyBucketLocalState state) {
        System.arraycopy(state.state, 0, this.state, 0, this.state.length);
    }

    public long getAvailableTokens(LeakyBucketConfiguration configuration) {
        Bandwidth[] limitedBandwidths = configuration.getLimitedBandwidths();
        long availableByLimitation = getCurrentSize(FIRST_LIMITED_OFFSET);
        for (int i = 1; i < limitedBandwidths.length; i++) {
            long currentTokens = getCurrentSize(FIRST_LIMITED_OFFSET + i);
            availableByLimitation = Math.min(currentTokens, availableByLimitation);
        }

        Bandwidth guaranteedBandwidth = configuration.getGuaranteedBandwidth();
        if (guaranteedBandwidth == null) {
            return availableByLimitation;
        }

        long availableByGuarantee = getCurrentSize(GUARANTEED_OFFSET);
        return Math.max(availableByLimitation, availableByGuarantee);
    }

    public void consume(long toConsume) {
        for (int i = GUARANTEED_OFFSET; i < state.length; i++) {
            state[i]= Math.max(0, state[i] - toConsume);
        }
    }

    public long calculateTimeToCloseDeficit(LeakyBucketConfiguration configuration, long deficit) {
        Bandwidth[] limitedBandwidths = configuration.getLimitedBandwidths();
        long sleepToRefill = timeRequiredToRefill(configuration, limitedBandwidths[0], deficit);
        for (int i = 1; i < limitedBandwidths.length; i++) {
            long currentSleepProposal = timeRequiredToRefill(configuration, limitedBandwidths[i], deficit);
            if (currentSleepProposal > sleepToRefill) {
                sleepToRefill = currentSleepProposal;
            }
        }

        Bandwidth guaranteedBandwidth = configuration.getGuaranteedBandwidth();
        if (guaranteedBandwidth != null && guaranteedBandwidth.getMaxCapacity() - state[GUARANTEED_OFFSET] >= deficit) {
            long guaranteedSleepToRefill = timeRequiredToRefill(configuration, guaranteedBandwidth, deficit);
            if (guaranteedSleepToRefill < sleepToRefill) {
                sleepToRefill = guaranteedSleepToRefill;
            }
        }

        return sleepToRefill;
    }

    public long timeRequiredToRefill(LeakyBucketConfiguration configuration, Bandwidth bandwidth, long numTokens) {
        return configuration.getRefillStrategy().timeRequiredToRefill(bandwidth, numTokens);
    }

}
