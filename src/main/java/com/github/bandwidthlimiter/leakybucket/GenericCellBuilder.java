package com.github.bandwidthlimiter.leakybucket;

import com.github.bandwidthlimiter.util.TimeMetter;
import com.github.bandwidthlimiter.leakybucket.local.ThreadSafeGenericCell;
import com.github.bandwidthlimiter.leakybucket.local.UnsafeGenericCell;
import com.github.bandwidthlimiter.util.WaitingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class GenericCellBuilder {

    private RefillStrategy refillStrategy = RefillStrategy.GENERIC_CELL_RATE;
    private WaitingStrategy waitingStrategy = WaitingStrategy.PARKING;
    private TimeMetter timeWrapper = TimeMetter.SYSTEM_MILLIS;
    private boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth = false;

    private Bandwidth guaranteedBandwidth;
    private List<Bandwidth> limitedBandwidths = new ArrayList<>(1);

    public LeakyBucket build() {
        GenericCellConfiguration configuration = createConfiguration();
        return new ThreadSafeGenericCell(configuration);
    }

    public LeakyBucket buildUnsafe() {
        GenericCellConfiguration configuration = createConfiguration();
        return new UnsafeGenericCell(configuration);
    }

    public GenericCellBuilder withGuaranteedBandwidth(long capacity, long period, TimeUnit timeUnit) {
        return withGuaranteedBandwidth(capacity, period, timeUnit, capacity);
    }

    public GenericCellBuilder withGuaranteedBandwidth(long capacity, long period, TimeUnit timeUnit, long initialCapacity) {
        return withGuaranteedBandwidth(new Bandwidth(capacity, initialCapacity, period, timeUnit));
    }

    public GenericCellBuilder withGuaranteedBandwidth(Bandwidth bandwidth) {
        if (this.guaranteedBandwidth != null) {
            throw LeakyBucketExceptions.onlyOneGuarantedBandwidthSupported();
        }
        this.guaranteedBandwidth = bandwidth;
        return this;
    }

    public GenericCellBuilder withLimitedBandwidth(long capacity, long period, TimeUnit timeUnit) {
        return withLimitedBandwidth(capacity, period, timeUnit, capacity);
    }

    public GenericCellBuilder withLimitedBandwidth(long capacity, long period, TimeUnit timeUnit, long initialCapacity) {
        return withLimitedBandwidth(new Bandwidth(capacity, initialCapacity, period, timeUnit));
    }

    public GenericCellBuilder withLimitedBandwidth(Bandwidth bandwidth) {
        limitedBandwidths.add(bandwidth);
        return this;
    }

    public GenericCellBuilder withNanoTimeWrapper(TimeMetter timeWrapper) {
        this.timeWrapper = timeWrapper;
        return this;
    }

    public GenericCellBuilder raiseErrorWhenConsumeGreaterThanSmallestBandwidth() {
        this.raiseErrorWhenConsumeGreaterThanSmallestBandwidth = true;
        return this;
    }

    public GenericCellBuilder withRefillStrategy(RefillStrategy refillStrategy) {
        this.refillStrategy = refillStrategy;
        return this;
    }

    public GenericCellBuilder withWaitingStrategy(WaitingStrategy waitingStrategy) {
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

    private GenericCellConfiguration createConfiguration() {
        Bandwidth[] restricteds = buildRestricted();
        return new GenericCellConfiguration(restricteds, guaranteedBandwidth,
                raiseErrorWhenConsumeGreaterThanSmallestBandwidth, timeWrapper, refillStrategy, waitingStrategy);
    }

}
