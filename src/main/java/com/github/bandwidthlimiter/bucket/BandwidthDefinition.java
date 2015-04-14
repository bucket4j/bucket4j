package com.github.bandwidthlimiter.bucket;

import com.github.bandwidthlimiter.bucket.BandwidthAdjuster.ImmutableCapacity;

import static com.github.bandwidthlimiter.bucket.BucketExceptions.*;

public class BandwidthDefinition {

    final long capacity;
    final BandwidthAdjuster adjuster;
    final long initialCapacity;
    final long period;
    final boolean guaranteed;
    final boolean limited;

    public BandwidthDefinition(long capacity, long initialCapacity, long period, boolean guaranteed) {
        if (capacity <= 0) {
            throw BucketExceptions.nonPositiveCapacity(capacity);
        }
        if (initialCapacity < 0) {
            throw nonPositiveInitialCapacity(initialCapacity);
        }
        if (period <= 0) {
            throw nonPositivePeriod(period);
        }
        this.capacity = capacity;
        this.adjuster = null;
        this.initialCapacity = initialCapacity;
        this.period = period;
        this.guaranteed = guaranteed;
        this.limited = !guaranteed;
    }

    public BandwidthDefinition(BandwidthAdjuster adjuster, long initialCapacity, long period, boolean guaranteed) {
        if (initialCapacity < 0) {
            throw nonPositiveInitialCapacity(initialCapacity);
        }
        if (period <= 0) {
            throw nonPositivePeriod(period);
        }
        if (adjuster == null) {
            throw nullBandwidthAdjuster();
        }
        this.capacity = 0;
        this.adjuster = adjuster;
        this.initialCapacity = initialCapacity;
        this.period = period;
        this.guaranteed = guaranteed;
        this.limited = !guaranteed;
    }

    public Bandwidth createBandwidth(int offset) {
        BandwidthAdjuster bandwidthAdjuster = adjuster != null ? adjuster : new ImmutableCapacity(capacity);
        return new Bandwidth(offset, bandwidthAdjuster, initialCapacity, period, guaranteed);
    }

    boolean hasDynamicCapacity() {
        return adjuster != null;
    }

    public double getTimeUnitsPerToken() {
        return (double) period / (double) capacity;
    }

    public double getTokensPerTimeUnit() {
        return (double) capacity / (double) period;
    }

}
