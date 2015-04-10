package com.github.bandwidthlimiter.bucket;

import com.github.bandwidthlimiter.bucket.local.ThreadSafeBucket;
import com.github.bandwidthlimiter.bucket.local.UnsafeBucket;

import java.util.ArrayList;
import java.util.List;

public final class BucketBuilder {

    private RefillStrategy refillStrategy = GenericCellRateRefillStrategy.INSTANCE;
    private TimeMeter timeMeter = TimeMeter.SYSTEM_NANOTIME;
    private boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth = false;
    private List<Bandwidth> bandwidths = new ArrayList<>(1);

    public BucketBuilder(TimeMeter timeMeter) {
        this.timeMeter = timeMeter;
    }

    public Bucket build() {
        BucketConfiguration configuration = createConfiguration();
        return new ThreadSafeBucket(configuration);
    }

    public Bucket buildUnsafe() {
        BucketConfiguration configuration = createConfiguration();
        return new UnsafeBucket(configuration);
    }

    public BucketBuilder withGuaranteedBandwidth(long capacity, long period) {
        return withGuaranteedBandwidth(capacity, period, capacity);
    }

    public BucketBuilder withGuaranteedBandwidth(long capacity, long period, long initialCapacity) {
        final Bandwidth bandwidth = new Bandwidth(bandwidths.size(), capacity, initialCapacity, period, true);
        bandwidths.add(bandwidth);
        return this;
    }

    public BucketBuilder withLimitedBandwidth(long capacity, long period) {
        return withLimitedBandwidth(capacity, period, capacity);
    }

    public BucketBuilder withLimitedBandwidth(long capacity, long period, long initialCapacity) {
        final Bandwidth bandwidth = new Bandwidth(bandwidths.size(), capacity, initialCapacity, period, false);
        bandwidths.add(bandwidth);
        return this;
    }

    public BucketBuilder raiseErrorWhenConsumeGreaterThanSmallestBandwidth() {
        this.raiseErrorWhenConsumeGreaterThanSmallestBandwidth = true;
        return this;
    }

    public BucketBuilder withCustomRefillStrategy(RefillStrategy refillStrategy) {
        this.refillStrategy = refillStrategy;
        return this;
    }

    public BucketBuilder withGenericCellRateRefillStrategy() {
        this.refillStrategy = GenericCellRateRefillStrategy.INSTANCE;
        return this;
    }

    private BucketConfiguration createConfiguration() {
        Bandwidth[] bandwidths = new Bandwidth[this.bandwidths.size()];
        for (int i = 0; i < bandwidths.length; i++) {
            bandwidths[i] = this.bandwidths.get(i);
        }
        return new BucketConfiguration(bandwidths, raiseErrorWhenConsumeGreaterThanSmallestBandwidth, timeMeter, refillStrategy);
    }

    public Bandwidth getBandwidth(int indexInTheBucket) {
        return bandwidths.get(indexInTheBucket);
    }

}
