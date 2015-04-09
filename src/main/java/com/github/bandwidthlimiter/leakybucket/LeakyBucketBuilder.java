package com.github.bandwidthlimiter.leakybucket;

import com.github.bandwidthlimiter.leakybucket.local.ThreadSafeLeakyBucket;
import com.github.bandwidthlimiter.leakybucket.local.UnsafeLeakyBucket;

import java.util.ArrayList;
import java.util.List;

public final class LeakyBucketBuilder {

    private RefillStrategy refillStrategy = GenericCellRateRefillStrategy.INSTANCE;
    private TimeMeter timeMeter = TimeMeter.SYSTEM_NANOTIME;
    private boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth = false;
    private List<Bandwidth> bandwidths = new ArrayList<>(1);

    public LeakyBucketBuilder(TimeMeter timeMeter) {
        this.timeMeter = timeMeter;
    }

    public LeakyBucket build() {
        LeakyBucketConfiguration configuration = createConfiguration();
        return new ThreadSafeLeakyBucket(configuration);
    }

    public LeakyBucket buildUnsafe() {
        LeakyBucketConfiguration configuration = createConfiguration();
        return new UnsafeLeakyBucket(configuration);
    }

    public LeakyBucketBuilder withGuaranteedBandwidth(long capacity, long period) {
        return withGuaranteedBandwidth(capacity, period, capacity);
    }

    public LeakyBucketBuilder withGuaranteedBandwidth(long capacity, long period, long initialCapacity) {
        final Bandwidth bandwidth = new Bandwidth(bandwidths.size(), capacity, initialCapacity, period, true);
        bandwidths.add(bandwidth);
        return this;
    }

    public LeakyBucketBuilder withLimitedBandwidth(long capacity, long period) {
        return withLimitedBandwidth(capacity, period, capacity);
    }

    public LeakyBucketBuilder withLimitedBandwidth(long capacity, long period, long initialCapacity) {
        final Bandwidth bandwidth = new Bandwidth(bandwidths.size(), capacity, initialCapacity, period, false);
        bandwidths.add(bandwidth);
        return this;
    }

    public LeakyBucketBuilder raiseErrorWhenConsumeGreaterThanSmallestBandwidth() {
        this.raiseErrorWhenConsumeGreaterThanSmallestBandwidth = true;
        return this;
    }

    public LeakyBucketBuilder withCustomRefillStrategy(RefillStrategy refillStrategy) {
        this.refillStrategy = refillStrategy;
        return this;
    }

    public LeakyBucketBuilder withGenericCellRateRefillStrategy() {
        this.refillStrategy = GenericCellRateRefillStrategy.INSTANCE;
        return this;
    }

    public LeakyBucketBuilder withTokenBucketRefillStrategy() {
        this.refillStrategy = TokenBucketRefillStrategy.INSTANCE;
        return this;
    }

    private LeakyBucketConfiguration createConfiguration() {
        Bandwidth[] bandwidths = new Bandwidth[this.bandwidths.size()];
        for (int i = 0; i < bandwidths.length; i++) {
            bandwidths[i] = this.bandwidths.get(i);
        }
        return new LeakyBucketConfiguration(bandwidths, raiseErrorWhenConsumeGreaterThanSmallestBandwidth, timeMeter, refillStrategy);
    }

    public Bandwidth getBandwidth(int indexInTheBucket) {
        return bandwidths.get(indexInTheBucket);
    }

}
