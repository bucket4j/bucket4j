package com.github.bandwidthlimiter.leakybucket;

import com.github.bandwidthlimiter.util.TimeMetter;
import com.github.bandwidthlimiter.util.WaitingStrategy;

import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.*;

public class LeakyBucketConfiguration {

    private final RefillStrategy refillStrategy;
    private final WaitingStrategy waitingStrategy;
    private final Bandwidth[] limitedBandwidths;
    private final Bandwidth guaranteedBandwidth;
    private final boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
    private final TimeMetter timeMetter;

    public LeakyBucketConfiguration(Bandwidth[] limitedBandwidths, Bandwidth guaranteedBandwidth,
            boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth, TimeMetter timeMetter,
            RefillStrategy refillStrategy, WaitingStrategy waitingStrategy) {
        if (timeMetter == null) {
            throw nullNanoTimeWrapper();
        }

        Bandwidth.checkBandwidths(limitedBandwidths, guaranteedBandwidth);

        this.limitedBandwidths = limitedBandwidths;
        this.guaranteedBandwidth = guaranteedBandwidth;
        this.raiseErrorWhenConsumeGreaterThanSmallestBandwidth = raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
        this.timeMetter = timeMetter;

        if (refillStrategy == null) {
            throw LeakyBucketExceptions.nullRefillStrategy();
        }
        if (waitingStrategy == null) {
            throw LeakyBucketExceptions.nullWaitingStrategy();
        }
        this.refillStrategy = refillStrategy;
        this.waitingStrategy = waitingStrategy;
    }

    public TimeMetter getTimeMetter() {
        return timeMetter;
    }

    public Bandwidth getGuaranteedBandwidth() {
        return guaranteedBandwidth;
    }

    public Bandwidth[] getLimitedBandwidths() {
        return limitedBandwidths;
    }

    public boolean isRaiseErrorWhenConsumeGreaterThanSmallestBandwidth() {
        return raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
    }

    public RefillStrategy getRefillStrategy() {
        return refillStrategy;
    }

    public WaitingStrategy getWaitingStrategy() {
        return waitingStrategy;
    }

}
