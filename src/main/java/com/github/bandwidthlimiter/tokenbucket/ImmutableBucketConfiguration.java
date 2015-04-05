package com.github.bandwidthlimiter.tokenbucket;

import com.github.bandwidthlimiter.NanoTimeWrapper;

import static com.github.bandwidthlimiter.tokenbucket.TokenBucketExceptions.*;

public class ImmutableBucketConfiguration {

    private final BandwidthDefinition[] limitedBandwidths;
    private final BandwidthDefinition guaranteedBandwidth;
    private final boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
    private final NanoTimeWrapper nanoTimeWrapper;

    public ImmutableBucketConfiguration(BandwidthDefinition[] limitedBandwidths, BandwidthDefinition guaranteedBandwidth, boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth, NanoTimeWrapper nanoTimeWrapper) {
        if (nanoTimeWrapper == null) {
            throw nullNanoTimeWrapper();
        }

        BandwidthDefinition.checkBandwidths(limitedBandwidths, guaranteedBandwidth);

        this.limitedBandwidths = limitedBandwidths;
        this.guaranteedBandwidth = guaranteedBandwidth;
        this.raiseErrorWhenConsumeGreaterThanSmallestBandwidth = raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
        this.nanoTimeWrapper = nanoTimeWrapper;
    }

    public NanoTimeWrapper getNanoTimeWrapper() {
        return nanoTimeWrapper;
    }

    public BandwidthDefinition getGuaranteedBandwidth() {
        return guaranteedBandwidth;
    }

    public BandwidthDefinition[] getLimitedBandwidths() {
        return limitedBandwidths;
    }

    public boolean isRaiseErrorWhenConsumeGreaterThanSmallestBandwidth() {
        return raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
    }

}
