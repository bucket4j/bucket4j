package com.github.bandwidthlimiter.genericcellrate;

import com.github.bandwidthlimiter.NanoTimeWrapper;

import static com.github.bandwidthlimiter.genericcellrate.TokenBucketExceptions.*;

public class ImmutableConfiguration {

    private final Bandwidth[] limitedBandwidths;
    private final Bandwidth guaranteedBandwidth;
    private final boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
    private final NanoTimeWrapper nanoTimeWrapper;

    public ImmutableConfiguration(Bandwidth[] limitedBandwidths, Bandwidth guaranteedBandwidth, boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth, NanoTimeWrapper nanoTimeWrapper) {
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
