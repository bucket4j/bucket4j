package com.github.bandwidthlimiter.leakybucket.local;

import com.github.bandwidthlimiter.leakybucket.Bandwidth;
import com.github.bandwidthlimiter.leakybucket.LeakyBucketConfiguration;
import com.github.bandwidthlimiter.leakybucket.GenericCellConfiguration;
import com.github.bandwidthlimiter.leakybucket.LeakyBucketState;

import java.util.Arrays;

public class LeakyBucketLocalState implements LeakyBucketState {

    private static final int REFILL_DATE_OFFSET = 0;
    private static final int GUARANTEED_OFFSET = 1;
    private static final int FIRST_LIMITED_OFFSET = 2;

    private final long[] state;

    @Override
    public long getRefillMarker(int bandwidthIndex) {
        return 0;
    }

    @Override
    public void setRefillMarker(int bandwidthIndex, long refillMarker) {

    }

    @Override
    public long getCurrentSize(int bandwidthIndex) {
        return 0;
    }

    @Override
    public void setCurrentSize(int bandwidthIndex, long size) {

    }

    LeakyBucketLocalState(LeakyBucketConfiguration configuration) {
        Bandwidth[] limitedBandwidths = configuration.getLimitedBandwidths();
        this.state = new long[2 + limitedBandwidths.length];
        for (int i = 0; i < limitedBandwidths.length; i++) {
            state[FIRST_LIMITED_OFFSET + i] = limitedBandwidths[i].getInitialCapacity();
        }

        Bandwidth guaranteedBandwidth = configuration.getGuaranteedBandwidth();
        state[GUARANTEED_OFFSET] = guaranteedBandwidth != null? guaranteedBandwidth.getInitialCapacity(): 0;

        state[REFILL_DATE_OFFSET] = configuration.getTimeMetter().time();
    }

    LeakyBucketLocalState(LeakyBucketLocalState previousState) {
        this.state = Arrays.copyOf(previousState.state, previousState.state.length);
    }

    void copyState(LeakyBucketLocalState state) {
        System.arraycopy(state.state, 0, this.state, 0, this.state.length);
    }

    public long getAvailableTokens(LeakyBucketConfiguration configuration) {
        for (int i = 0; i < ) {

        }
    }

    public void refill(long currentNanoTime, LeakyBucketConfiguration configuration) {
        Bandwidth guaranteedBandwidth = configuration.getGuaranteedBandwidth();
        Bandwidth[] limitedBandwidths = configuration.getLimitedBandwidths();
        long previousRefillNanos = state[REFILL_DATE_OFFSET];

        long availableByGuarantee = 0l;
        if (guaranteedBandwidth != null) {
            availableByGuarantee = state[GUARANTEED_OFFSET] + refill(configuration, guaranteedBandwidth, previousRefillNanos, currentNanoTime);
            availableByGuarantee = Math.min(availableByGuarantee, guaranteedBandwidth.getMaxCapacity());
            state[GUARANTEED_OFFSET] = availableByGuarantee;
        }

        long availableByLimitation = Long.MAX_VALUE;
        for (int i = 0; i < limitedBandwidths.length; i++) {
            Bandwidth bandwidth = limitedBandwidths[i];
            long newSize = state[FIRST_LIMITED_OFFSET + i] + refill(configuration, bandwidth, previousRefillNanos, currentNanoTime);
            newSize = Math.min(newSize, bandwidth.getMaxCapacity());
            state[FIRST_LIMITED_OFFSET + i] = newSize;
            availableByLimitation = Math.min(newSize, availableByLimitation);
        }

        state[REFILL_DATE_OFFSET] = currentNanoTime;

        return Math.max(availableByGuarantee, availableByLimitation);
    }

    public void consume(long toConsume) {
        for (int i = GUARANTEED_OFFSET; i < state.length; i++) {
            state[i]= Math.max(0, state[i] - toConsume);
        }
    }

    public boolean sleepUntilRefillIfPossible(long deficit, long sleepLimitNanos, LeakyBucketConfiguration configuration) throws InterruptedException {
        Bandwidth guaranteedBandwidth = configuration.getGuaranteedBandwidth();
        Bandwidth[] limitedBandwidths = configuration.getLimitedBandwidths();

        long sleepToRefill = timeRequiredToRefill(configuration, limitedBandwidths[0], deficit);
        for (int i = 1; i < limitedBandwidths.length; i++) {
            long currentSleepProposal = timeRequiredToRefill(configuration, limitedBandwidths[i], deficit);
            if (currentSleepProposal > sleepToRefill) {
                sleepToRefill = currentSleepProposal;
            }
        }

        if (guaranteedBandwidth != null && guaranteedBandwidth.getMaxCapacity() - state[GUARANTEED_OFFSET] >= deficit) {
            long guaranteedSleepToRefill = timeRequiredToRefill(configuration, guaranteedBandwidth, deficit);
            if (guaranteedSleepToRefill < sleepToRefill) {
                sleepToRefill = guaranteedSleepToRefill;
            }
        }

        if (sleepToRefill >= sleepLimitNanos) {
            return false;
        }

        sleep(configuration, sleepToRefill);
        return true;
    }

    public void sleep(LeakyBucketConfiguration configuration, long timeToAwait) throws InterruptedException {
        configuration.getTimeMetter().sleep(timeToAwait);
    }

    public long timeRequiredToRefill(LeakyBucketConfiguration configuration, Bandwidth bandwidth, long numTokens) {
        return configuration.getRefillStrategy().timeRequiredToRefill(bandwidth, numTokens);
    }

}
