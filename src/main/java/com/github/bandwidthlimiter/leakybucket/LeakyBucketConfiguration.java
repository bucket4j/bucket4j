package com.github.bandwidthlimiter.leakybucket;

import com.github.bandwidthlimiter.util.NanoTimeWrapper;

import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.*;

public class LeakyBucketConfiguration {

    private final Bandwidth[] limitedBandwidths;
    private final Bandwidth guaranteedBandwidth;
    private final boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
    private final NanoTimeWrapper nanoTimeWrapper;

    public LeakyBucketConfiguration(Bandwidth[] limitedBandwidths, Bandwidth guaranteedBandwidth, boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth, NanoTimeWrapper nanoTimeWrapper) {
        if (nanoTimeWrapper == null) {
            throw nullNanoTimeWrapper();
        }

        Bandwidth.checkBandwidths(limitedBandwidths, guaranteedBandwidth);

        this.limitedBandwidths = limitedBandwidths;
        this.guaranteedBandwidth = guaranteedBandwidth;
        this.raiseErrorWhenConsumeGreaterThanSmallestBandwidth = raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
        this.nanoTimeWrapper = nanoTimeWrapper;
    }

    public NanoTimeWrapper getNanoTimeWrapper() {
        return nanoTimeWrapper;
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

}
