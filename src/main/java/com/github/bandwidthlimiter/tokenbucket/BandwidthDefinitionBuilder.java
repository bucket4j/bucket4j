package com.github.bandwidthlimiter.tokenbucket;

import java.util.concurrent.TimeUnit;

public class BandwidthDefinitionBuilder {

    private final TokenBucketBuilder rootBuilder;

    private RefillStrategy refillStrategy = RefillStrategy.CONTINUOUS;
    private WaitingStrategy waitingStrategy = WaitingStrategy.PARKING;
    private Long initialCapacity = null;

    private long capacity;
    private long period;
    private TimeUnit timeUnit;

    public BandwidthDefinitionBuilder(TokenBucketBuilder rootBuilder) {
        this.rootBuilder = rootBuilder;
    }

    // visible for testing
    BandwidthDefinitionBuilder() {
        rootBuilder = null;
    }

    public BandwidthDefinitionBuilder withCapacity(long capacity) {
        this.capacity = capacity;
        return this;
    }

    public BandwidthDefinitionBuilder withDuration(long period, TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        this.period = period;
        return this;
    }

    public BandwidthDefinitionBuilder withInitialCapacity(long initialCapacity) {
        this.initialCapacity = initialCapacity;
        return this;
    }

    public BandwidthDefinitionBuilder withRefillStrategy(RefillStrategy refillStrategy) {
        this.refillStrategy = refillStrategy;
        return this;
    }

    public BandwidthDefinitionBuilder withWaitingStrategy(WaitingStrategy waitingStrategy) {
        this.waitingStrategy = waitingStrategy;
        return this;
    }

    public BandwidthDefinitionBuilder setupGuaranteedBandwidth() {
        return this.rootBuilder.setupGuaranteedBandwidth();
    }

    public BandwidthDefinitionBuilder addLimitedBandwidth() {
        return this.rootBuilder.setupGuaranteedBandwidth();
    }

    public BandwidthDefinition buildBandwidth() {
        long initialCapacity = this.initialCapacity == null? capacity: this.initialCapacity.longValue();
        return new BandwidthDefinition(capacity, initialCapacity, period, timeUnit, refillStrategy, waitingStrategy);
    }


}
