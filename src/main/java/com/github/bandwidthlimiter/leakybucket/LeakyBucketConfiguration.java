package com.github.bandwidthlimiter.leakybucket;

import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.*;

public class LeakyBucketConfiguration {

    private final RefillStrategy refillStrategy;
    private final Bandwidth[] limitedBandwidths;
    private final Bandwidth guaranteedBandwidth;
    private final Bandwidth[] allBandwidths;
    private final boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
    private final TimeMetter timeMetter;

    public LeakyBucketConfiguration(Bandwidth[] limitedBandwidths, Bandwidth guaranteedBandwidth,
            boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth, TimeMetter timeMetter,
            RefillStrategy refillStrategy) {
        if (timeMetter == null) {
            throw nullTimeMetter();
        }
        if (refillStrategy == null) {
            throw LeakyBucketExceptions.nullRefillStrategy();
        }
        this.refillStrategy = refillStrategy;

        Bandwidth.checkBandwidths(limitedBandwidths, guaranteedBandwidth);

        if (guaranteedBandwidth == null) {
            this.allBandwidths = limitedBandwidths;
        } else {
            this.allBandwidths = new Bandwidth[1 + limitedBandwidths.length];
            this.allBandwidths[0] = guaranteedBandwidth;
            for (int i = 0; i < limitedBandwidths.length; i++) {
                this.allBandwidths[i + 1] = limitedBandwidths[i];
            }
        }

        this.limitedBandwidths = limitedBandwidths;
        this.guaranteedBandwidth = guaranteedBandwidth;
        this.raiseErrorWhenConsumeGreaterThanSmallestBandwidth = raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
        this.timeMetter = timeMetter;
    }

    public TimeMetter getTimeMetter() {
        return timeMetter;
    }

    public Bandwidth getGuaranteedBandwidth() {
        return guaranteedBandwidth;
    }

    public boolean hasGuaranteedBandwidth() {
        return guaranteedBandwidth != null;
    }

    public int getLimitedBandwidthsCount() {
        return limitedBandwidths.length;
    }

    public Bandwidth[] getLimitedBandwidths() {
        return limitedBandwidths;
    }

    public Bandwidth[] getAllBandwidths() {
        return allBandwidths;
    }

    public Bandwidth getBandwidths(int bandwidthIndex) {
        return allBandwidths[bandwidthIndex];
    }

    public int getBandwidthCount() {
        return allBandwidths.length;
    }

    public boolean isRaiseErrorWhenConsumeGreaterThanSmallestBandwidth() {
        return raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
    }

    public RefillStrategy getRefillStrategy() {
        return refillStrategy;
    }
}
