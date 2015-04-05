package com.github.bandwidthlimiter.genericcellrate;

import com.github.bandwidthlimiter.NanoTimeWrapper;
import com.github.bandwidthlimiter.genericcellrate.local.ThreadSafeTokenBucket;
import com.github.bandwidthlimiter.genericcellrate.local.UnsafeTokenBucket;

import java.util.ArrayList;
import java.util.List;

public final class TokenBucketBuilder {

    private NanoTimeWrapper timeWrapper = NanoTimeWrapper.SYSTEM;
    private boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth = false;
    private BandwidthBuilder guaranteedBuilder;
    private List<BandwidthBuilder> limitedBuilders = new ArrayList<>(1);

    public TokenBucket build() {
        Bandwidth[] restricteds = buildRestricted();
        Bandwidth guaranteed = buildGuaranteed();
        ImmutableConfiguration configuration = new ImmutableConfiguration(restricteds, guaranteed, raiseErrorWhenConsumeGreaterThanSmallestBandwidth, timeWrapper);
        return new ThreadSafeTokenBucket(configuration);
    }

    public TokenBucket buildUnsafe() {
        Bandwidth[] restricteds = buildRestricted();
        Bandwidth guaranteed = buildGuaranteed();
        ImmutableConfiguration configuration = new ImmutableConfiguration(restricteds, guaranteed, raiseErrorWhenConsumeGreaterThanSmallestBandwidth, timeWrapper);
        return new UnsafeTokenBucket(configuration);
    }

    public BandwidthBuilder setupGuaranteedBandwidth() {
        if (this.guaranteedBuilder != null) {
            throw TokenBucketExceptions.onlyOneGuarantedBandwidthSupported();
        }
        this.guaranteedBuilder = new BandwidthBuilder(this);
        return this.guaranteedBuilder;
    }

    public TokenBucketBuilder setGuaranteedBandwidth(BandwidthBuilder guaranteedBuilder) {
        if (this.guaranteedBuilder != null) {
            throw TokenBucketExceptions.onlyOneGuarantedBandwidthSupported();
        }
        this.guaranteedBuilder = guaranteedBuilder;
        return this;
    }

    public BandwidthBuilder addLimitedBandwidth() {
        limitedBuilders.add(new BandwidthBuilder(this));
        return limitedBuilders.get(limitedBuilders.size() - 1);
    }

    public TokenBucketBuilder addLimitedBandwidth(BandwidthBuilder limitedBuilder) {
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

    private Bandwidth[] buildRestricted() {
        Bandwidth[] result = new Bandwidth[limitedBuilders.size()];
        for (int i = 0; i < limitedBuilders.size(); i++) {
            result[i] = limitedBuilders.get(i).buildBandwidth();
        }
        return result;
    }

    private Bandwidth buildGuaranteed() {
        return guaranteedBuilder == null? null: guaranteedBuilder.buildBandwidth();
    }

}
