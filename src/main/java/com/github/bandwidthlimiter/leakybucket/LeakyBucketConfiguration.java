package com.github.bandwidthlimiter.leakybucket;

import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.*;

public class LeakyBucketConfiguration {

    private final RefillStrategy refillStrategy;
    private final Bandwidth[] bandwidths;
    private final boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
    private final TimeMetter timeMetter;

    public LeakyBucketConfiguration(Bandwidth[] bandwidths, boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth,
            TimeMetter timeMetter, RefillStrategy refillStrategy) {
        if (timeMetter == null) {
            throw nullTimeMetter();
        }
        this.timeMetter = timeMetter;

        if (refillStrategy == null) {
            throw nullRefillStrategy();
        }
        this.refillStrategy = refillStrategy;

        Bandwidth.checkBandwidths(bandwidths);
        this.bandwidths = bandwidths;
        this.raiseErrorWhenConsumeGreaterThanSmallestBandwidth = raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
    }

    public TimeMetter getTimeMetter() {
        return timeMetter;
    }

    public RefillStrategy getRefillStrategy() {
        return refillStrategy;
    }

    public Bandwidth[] getBandwidths() {
        return bandwidths;
    }

    public Bandwidth getBandwidths(int bandwidthIndex) {
        return bandwidths[bandwidthIndex];
    }

    public int getBandwidthCount() {
        return bandwidths.length;
    }

    public boolean isRaiseErrorWhenConsumeGreaterThanSmallestBandwidth() {
        return raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
    }

}
