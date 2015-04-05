package com.github.bandwidthlimiter.genericcellrate;

import java.util.concurrent.TimeUnit;

public class BandwidthBuilder {

    private final TokenBucketBuilder rootBuilder;

    private RefillStrategy refillStrategy = RefillStrategy.MONOTONE;
    private WaitingStrategy waitingStrategy = WaitingStrategy.PARKING;
    private Long initialCapacity = null;

    private long capacity;
    private long period;
    private TimeUnit timeUnit;

    public BandwidthBuilder(TokenBucketBuilder rootBuilder) {
        this.rootBuilder = rootBuilder;
    }

    // visible for testing
    BandwidthBuilder() {
        rootBuilder = null;
    }

    public BandwidthBuilder withCapacity(long capacity) {
        this.capacity = capacity;
        return this;
    }

    public BandwidthBuilder withInterval(long period, TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        this.period = period;
        return this;
    }

    public BandwidthBuilder withInitialCapacity(long initialCapacity) {
        this.initialCapacity = initialCapacity;
        return this;
    }

    public BandwidthBuilder withRefillStrategy(RefillStrategy refillStrategy) {
        this.refillStrategy = refillStrategy;
        return this;
    }

    public BandwidthBuilder withWaitingStrategy(WaitingStrategy waitingStrategy) {
        this.waitingStrategy = waitingStrategy;
        return this;
    }

    public BandwidthBuilder setupGuaranteedBandwidth() {
        return this.rootBuilder.setupGuaranteedBandwidth();
    }

    public BandwidthBuilder addLimitedBandwidth() {
        return this.rootBuilder.setupGuaranteedBandwidth();
    }

    public TokenBucket build() {
        return rootBuilder.build();
    }

    Bandwidth buildBandwidth() {
        long initialCapacity = this.initialCapacity == null? capacity: this.initialCapacity.longValue();
        return new Bandwidth(capacity, initialCapacity, period, timeUnit, refillStrategy, waitingStrategy);
    }

}
