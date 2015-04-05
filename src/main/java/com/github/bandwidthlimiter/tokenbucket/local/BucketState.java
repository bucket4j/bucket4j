package com.github.bandwidthlimiter.tokenbucket.local;

import com.github.bandwidthlimiter.tokenbucket.BandwidthDefinition;
import com.github.bandwidthlimiter.tokenbucket.ImmutableBucketConfiguration;

import java.util.Arrays;

public class BucketState {

    private static final int REFILL_DATE_OFFSET = 0;
    private static final int GUARANTEED_OFFSET = 1;
    private static final int FIRST_LIMITED_OFFSET = 2;

    private final long[] state;

    BucketState(ImmutableBucketConfiguration configuration) {
        BandwidthDefinition[] limitedBandwidths = configuration.getLimitedBandwidths();
        this.state = new long[2 + limitedBandwidths.length];
        for (int i = 0; i < limitedBandwidths.length; i++) {
            state[FIRST_LIMITED_OFFSET + i] = limitedBandwidths[i].getInitialCapacity();
        }

        BandwidthDefinition guaranteedBandwidth = configuration.getGuaranteedBandwidth();
        state[GUARANTEED_OFFSET] = guaranteedBandwidth != null? guaranteedBandwidth.getInitialCapacity(): 0;

        state[REFILL_DATE_OFFSET] = configuration.getNanoTimeWrapper().nanoTime();
    }

    BucketState(BucketState previousState) {
        this.state = Arrays.copyOf(previousState.state, previousState.state.length);
    }

    void copyState(BucketState state) {
        System.arraycopy(state.state, 0, this.state, 0, this.state.length);
    }

    public long refill(long currentNanoTime, ImmutableBucketConfiguration configuration) {
        BandwidthDefinition guaranteedBandwidth = configuration.getGuaranteedBandwidth();
        BandwidthDefinition[] limitedBandwidths = configuration.getLimitedBandwidths();
        long previousRefillNanos = state[REFILL_DATE_OFFSET];

        long availableByGuarantee = 0l;
        if (guaranteedBandwidth != null) {
            availableByGuarantee = state[GUARANTEED_OFFSET] + guaranteedBandwidth.refill(previousRefillNanos, currentNanoTime);
            availableByGuarantee = Math.min(availableByGuarantee, guaranteedBandwidth.getMaxCapacity());
            state[GUARANTEED_OFFSET] = availableByGuarantee;
        }

        long availableByLimitation = Long.MAX_VALUE;
        for (int i = 0; i < limitedBandwidths.length; i++) {
            BandwidthDefinition bandwidth = limitedBandwidths[i];
            long newSize = state[FIRST_LIMITED_OFFSET + i] + bandwidth.refill(previousRefillNanos, currentNanoTime);
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

    public boolean sleepUntilRefillIfPossible(long deficit, long sleepLimitNanos, ImmutableBucketConfiguration configuration) throws InterruptedException {
        BandwidthDefinition guaranteedBandwidth = configuration.getGuaranteedBandwidth();
        BandwidthDefinition[] limitedBandwidths = configuration.getLimitedBandwidths();

        BandwidthDefinition bandwidthToSleep = limitedBandwidths[0];
        long sleepToRefill = bandwidthToSleep.nanosRequiredToRefill(deficit);
        for (int i = 1; i < limitedBandwidths.length; i++) {
            long currentSleepProposal = limitedBandwidths[i].nanosRequiredToRefill(deficit);
            if (currentSleepProposal > sleepToRefill) {
                sleepToRefill = currentSleepProposal;
                bandwidthToSleep = limitedBandwidths[i];
            }
        }

        if (guaranteedBandwidth != null && guaranteedBandwidth.getMaxCapacity() - state[GUARANTEED_OFFSET] >= deficit) {
            long guaranteedSleepToRefill = guaranteedBandwidth.nanosRequiredToRefill(deficit);
            if (guaranteedSleepToRefill < sleepToRefill) {
                bandwidthToSleep = guaranteedBandwidth;
                sleepToRefill = guaranteedSleepToRefill;
            }
        }

        if (sleepToRefill >= sleepLimitNanos) {
            return false;
        }

        bandwidthToSleep.sleep(sleepToRefill);
        return true;
    }

}
