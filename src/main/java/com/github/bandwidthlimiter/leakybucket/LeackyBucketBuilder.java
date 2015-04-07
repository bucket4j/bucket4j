package com.github.bandwidthlimiter.leakybucket;

import com.github.bandwidthlimiter.leakybucket.local.ThreadSafeGenericCell;
import com.github.bandwidthlimiter.leakybucket.local.UnsafeGenericCell;
import com.github.bandwidthlimiter.util.WaitingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class LeackyBucketBuilder {

    private RefillStrategy refillStrategy = GenericCellRateRefillStrategy.INSTANCE;
    private WaitingStrategy waitingStrategy = WaitingStrategy.PARKING;
    private TimeMetter timeWrapper = TimeMetter.SYSTEM_NANOTIME;
    private boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth = false;

    private Bandwidth guaranteedBandwidth;
    private List<Bandwidth> limitedBandwidths = new ArrayList<>(1);

    public LeakyBucket build() {
        LeakyBucketConfiguration configuration = createConfiguration();
        return new ThreadSafeGenericCell(configuration);
    }

    public LeakyBucket buildUnsafe() {
        LeakyBucketConfiguration configuration = createConfiguration();
        return new UnsafeGenericCell(configuration);
    }

    public LeackyBucketBuilder withGuaranteedBandwidth(long capacity, long period, TimeUnit timeUnit) {
        return withGuaranteedBandwidth(capacity, period, timeUnit, capacity);
    }

    public LeackyBucketBuilder withGuaranteedBandwidth(long capacity, long period, TimeUnit timeUnit, long initialCapacity) {
        return withGuaranteedBandwidth(new Bandwidth(capacity, initialCapacity, period, timeUnit));
    }

    public LeackyBucketBuilder withGuaranteedBandwidth(Bandwidth bandwidth) {
        if (this.guaranteedBandwidth != null) {
            throw LeakyBucketExceptions.onlyOneGuarantedBandwidthSupported();
        }
        this.guaranteedBandwidth = bandwidth;
        return this;
    }

    public LeackyBucketBuilder withLimitedBandwidth(long capacity, long period, TimeUnit timeUnit) {
        return withLimitedBandwidth(capacity, period, timeUnit, capacity);
    }

    public LeackyBucketBuilder withLimitedBandwidth(long capacity, long period, TimeUnit timeUnit, long initialCapacity) {
        return withLimitedBandwidth(new Bandwidth(capacity, initialCapacity, period, timeUnit));
    }

    public LeackyBucketBuilder withLimitedBandwidth(Bandwidth bandwidth) {
        limitedBandwidths.add(bandwidth);
        return this;
    }

    public LeackyBucketBuilder withNanosecondPrecision() {
        this.timeWrapper = TimeMetter.SYSTEM_NANOTIME;
        return this;
    }

    public LeackyBucketBuilder withMillisecondPrecision() {
        this.timeWrapper = TimeMetter.SYSTEM_MILLISECONDS;
        return this;
    }

    public LeackyBucketBuilder withCustomTimeMetter(TimeMetter timeWrapper) {
        this.timeWrapper = timeWrapper;
        return this;
    }

    public LeackyBucketBuilder raiseErrorWhenConsumeGreaterThanSmallestBandwidth() {
        this.raiseErrorWhenConsumeGreaterThanSmallestBandwidth = true;
        return this;
    }

    public LeackyBucketBuilder withCustomRefillStrategy(RefillStrategy refillStrategy) {
        this.refillStrategy = refillStrategy;
        return this;
    }

    public LeackyBucketBuilder withGenericCellRateRefillStrategy() {
        this.refillStrategy = GenericCellRateRefillStrategy.INSTANCE;
        return this;
    }

    public LeackyBucketBuilder withWaitingStrategy(WaitingStrategy waitingStrategy) {
        this.waitingStrategy = waitingStrategy;
        return this;
    }

    private Bandwidth[] buildRestricted() {
        Bandwidth[] result = new Bandwidth[limitedBandwidths.size()];
        for (int i = 0; i < limitedBandwidths.size(); i++) {
            result[i] = limitedBandwidths.get(i);
        }
        return result;
    }

    private LeakyBucketConfiguration createConfiguration() {
        Bandwidth[] restricteds = buildRestricted();
        return new LeakyBucketConfiguration(restricteds, guaranteedBandwidth,
                raiseErrorWhenConsumeGreaterThanSmallestBandwidth, timeWrapper, refillStrategy, waitingStrategy);
    }

}
