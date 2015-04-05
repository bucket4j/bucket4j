package com.github.bandwidthlimiter.leakybucket.genericcellrate;

import com.github.bandwidthlimiter.leakybucket.Bandwidth;
import com.github.bandwidthlimiter.leakybucket.LeakyBucketConfiguration;
import com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions;
import com.github.bandwidthlimiter.util.NanoTimeWrapper;
import com.github.bandwidthlimiter.util.WaitingStrategy;

public class GenericCellConfiguration extends LeakyBucketConfiguration {

    private final RefillStrategy refillStrategy;
    private final WaitingStrategy waitingStrategy;

    public GenericCellConfiguration(Bandwidth[] limitedBandwidths, Bandwidth guaranteedBandwidth,
            boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth, NanoTimeWrapper nanoTimeWrapper,
            RefillStrategy refillStrategy, WaitingStrategy waitingStrategy) {
        super(limitedBandwidths, guaranteedBandwidth, raiseErrorWhenConsumeGreaterThanSmallestBandwidth, nanoTimeWrapper);
        if (refillStrategy == null) {
            throw LeakyBucketExceptions.nullRefillStrategy();
        }
        if (waitingStrategy == null) {
            throw LeakyBucketExceptions.nullWaitingStrategy();
        }
        this.refillStrategy = refillStrategy;
        this.waitingStrategy = waitingStrategy;
    }

    public RefillStrategy getRefillStrategy() {
        return refillStrategy;
    }

    public WaitingStrategy getWaitingStrategy() {
        return waitingStrategy;
    }

}
