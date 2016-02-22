/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.bucket4j;

import java.time.Duration;

import static com.github.bucket4j.BucketExceptions.*;

public class BandwidthDefinition {

    final long capacity;
    final CapacityAdjuster adjuster;
    final long initialCapacity;
    final long periodNanos;
    final boolean guaranteed;
    final boolean limited;

    public BandwidthDefinition(long capacity, long initialCapacity, Duration period, boolean guaranteed) {
        this(validateCapacity(capacity), null, initialCapacity, period, guaranteed);
    }

    public BandwidthDefinition(CapacityAdjuster adjuster, long initialCapacity, Duration period, boolean guaranteed) {
        this(0l, validateAdjuster(adjuster), initialCapacity, period, guaranteed);
    }

    private BandwidthDefinition(long capacity, CapacityAdjuster adjuster, long initialCapacity, Duration period, boolean guaranteed) {
        long periodNanos = period.toNanos();
        if (initialCapacity < 0) {
            throw nonPositiveInitialCapacity(initialCapacity);
        }
        if (periodNanos <= 0) {
            throw nonPositivePeriod(periodNanos);
        }
        this.capacity = capacity;
        this.adjuster = adjuster;
        this.initialCapacity = initialCapacity;
        this.periodNanos = periodNanos;
        this.guaranteed = guaranteed;
        this.limited = !guaranteed;
    }

    public Bandwidth createBandwidth() {
        CapacityAdjuster capacityAdjuster = adjuster != null ? adjuster : new CapacityAdjuster.ImmutableCapacity(capacity);
        return new Bandwidth(capacityAdjuster, initialCapacity, periodNanos, guaranteed);
    }

    boolean hasDynamicCapacity() {
        return adjuster != null;
    }

    public double getTimeUnitsPerToken() {
        return (double) periodNanos / (double) capacity;
    }

    public double getTokensPerTimeUnit() {
        return (double) capacity / (double) periodNanos;
    }

    private static long validateCapacity(long capacity) {
        if (capacity <= 0) {
            throw nonPositiveCapacity(capacity);
        }
        return capacity;
    }

    private static CapacityAdjuster validateAdjuster(CapacityAdjuster adjuster) {
        if (adjuster == null) {
            throw nullBandwidthAdjuster();
        }
        return adjuster;
    }

    @Override
    public String toString() {
        return "BandwidthDefinition{" +
                "capacity=" + capacity +
                ", adjuster=" + adjuster +
                ", initialCapacity=" + initialCapacity +
                ", periodNanos=" + periodNanos +
                ", guaranteed=" + guaranteed +
                ", limited=" + limited +
                '}';
    }

}
