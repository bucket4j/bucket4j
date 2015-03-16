package com.github.bandwidthlimiter.tokenbucket;

import com.github.bandwidthlimiter.BandwidthLimiter;
import com.github.bandwidthlimiter.NanoTimeWrapper;

import java.util.ArrayList;
import java.util.List;

public final class TokenBucketBuilder {

    private NanoTimeWrapper timeWrapper = NanoTimeWrapper.SYSTEM;
    private boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth = false;
    private BandwidthDefinitionBuilder guaranteedBuilder;
    private List<BandwidthDefinitionBuilder> limitedBuilders = new ArrayList<>(1);

    public BandwidthLimiter build() {
        BandwidthDefinition[] restricteds = buildRestricted();
        BandwidthDefinition guaranteed = buildGuaranteed();
        return new TokenBucketBandwidthLimiter(restricteds, guaranteed, raiseErrorWhenConsumeGreaterThanSmallestBandwidth, timeWrapper);
    }

    public BandwidthDefinitionBuilder setupGuaranteedBandwidth() {
        if (this.guaranteedBuilder != null) {
            throw TokenBucketExceptions.onlyOneGuarantedBandwidthSupported();
        }
        this.guaranteedBuilder = new BandwidthDefinitionBuilder(this);
        return this.guaranteedBuilder;
    }

    public TokenBucketBuilder setGuaranteedBandwidth(BandwidthDefinitionBuilder guaranteedBuilder) {
        if (this.guaranteedBuilder != null) {
            throw TokenBucketExceptions.onlyOneGuarantedBandwidthSupported();
        }
        this.guaranteedBuilder = guaranteedBuilder;
        return this;
    }

    public BandwidthDefinitionBuilder addLimitedBandwidth() {
        limitedBuilders.add(new BandwidthDefinitionBuilder(this));
        return limitedBuilders.get(limitedBuilders.size() - 1);
    }

    public TokenBucketBuilder addLimitedBandwidth(BandwidthDefinitionBuilder limitedBuilder) {
        limitedBuilders.add(limitedBuilder);
        return this;
    }

    public TokenBucketBuilder withNanoTimeWrapper(NanoTimeWrapper timeWrapper) {
        this.timeWrapper = timeWrapper;
        return this;
    }

    public TokenBucketBuilder raiseErrorWhenConsumeGreaterThanSmallestBandwidth() {
        this.raiseErrorWhenConsumeGreaterThanSmallestBandwidth = true;
        return this;
    }

    private BandwidthDefinition[] buildRestricted() {
        BandwidthDefinition[] result = new BandwidthDefinition[limitedBuilders.size()];
        for (int i = 0; i < limitedBuilders.size(); i++) {
            result[i] = limitedBuilders.get(i).buildBandwidth();
        }
        return result;
    }

    private BandwidthDefinition buildGuaranteed() {
        return guaranteedBuilder == null? null: guaranteedBuilder.buildBandwidth();
    }

}
