/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.bandwidthlimiter.leakybucket;

import java.util.concurrent.TimeUnit;

import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.guarantedHasGreaterRateThanLimited;
import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.hasOverlaps;
import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.restrictionsNotSpecified;

public final class Bandwidth {

    private final long capacity;
    private final long initialCapacity;
    private final long period;
    private final TimeUnit timeUnit;
    private final long periodInNanos;
    private final double tokensGeneratedInOneNanosecond;
    private final double nanosecondsToGenerateOneToken;

    public Bandwidth(long capacity, long period, TimeUnit timeUnit) {
        this(capacity, capacity, period, timeUnit);
    }

    public Bandwidth(long capacity, long initialCapacity, long period, TimeUnit timeUnit) {
        if (capacity <= 0) {
            throw LeakyBucketExceptions.nonPositiveCapacity(capacity);
        }
        if (initialCapacity < 0) {
            throw LeakyBucketExceptions.nonPositiveInitialCapacity(initialCapacity);
        }
        if (initialCapacity > capacity) {
            throw LeakyBucketExceptions.initialCapacityGreaterThanMaxCapacity(initialCapacity, capacity);
        }
        if (timeUnit == null) {
            throw LeakyBucketExceptions.nullTimeUnit();
        }
        if (period <= 0) {
            throw LeakyBucketExceptions.nonPositivePeriod(period);
        }

        this.capacity = capacity;
        this.initialCapacity = initialCapacity;
        this.period = period;
        this.timeUnit = timeUnit;
        this.periodInNanos = timeUnit.toNanos(period);
        this.tokensGeneratedInOneNanosecond = (double) capacity / (double) periodInNanos;
        this.nanosecondsToGenerateOneToken = (double) periodInNanos / (double) capacity;
    }

    public long getInitialCapacity() {
        return initialCapacity;
    }

    public long getMaxCapacity() {
        return capacity;
    }

    public long getPeriodInNanos() {
        return periodInNanos;
    }

    public static long getSmallestCapacity(Bandwidth[] definitions) {
        long minCapacity = Long.MAX_VALUE;
        for (int i = 0; i < definitions.length; i++) {
            if (definitions[i].capacity < minCapacity) {
                minCapacity = definitions[i].capacity;
            }
        }
        return minCapacity;
    }

    public static void checkBandwidths(Bandwidth[] limitedBandwidths, Bandwidth guaranteedBandwidth) {
        if (limitedBandwidths == null || limitedBandwidths.length == 0) {
            throw restrictionsNotSpecified();
        }
        for (int i = 0; i < limitedBandwidths.length - 1; i++) {
            for (int j = 1; j < limitedBandwidths.length; j++) {
                Bandwidth first = limitedBandwidths[i];
                Bandwidth second = limitedBandwidths[j];
                if (first.periodInNanos < second.periodInNanos && first.capacity >= second.capacity) {
                    throw hasOverlaps(first, second);
                } else if (first.periodInNanos == second.periodInNanos) {
                    throw hasOverlaps(first, second);
                } else if (first.periodInNanos > second.periodInNanos && first.capacity <= second.capacity) {
                    throw hasOverlaps(first, second);
                }
            }
        }
        if (guaranteedBandwidth != null) {
            for (Bandwidth limited : limitedBandwidths) {
                if (limited.tokensGeneratedInOneNanosecond <= guaranteedBandwidth.tokensGeneratedInOneNanosecond
                        || limited.nanosecondsToGenerateOneToken > guaranteedBandwidth.nanosecondsToGenerateOneToken) {
                    throw guarantedHasGreaterRateThanLimited(guaranteedBandwidth, limited);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "BandwidthDefinition{" +
                "capacity=" + capacity +
                ", initialCapacity=" + initialCapacity +
                ", period=" + period +
                ", timeUnit=" + timeUnit +
                ", periodInNanos=" + periodInNanos +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bandwidth that = (Bandwidth) o;

        if (capacity != that.capacity) return false;
        if (initialCapacity != that.initialCapacity) return false;
        if (periodInNanos != that.periodInNanos) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (capacity ^ (capacity >>> 32));
        result = 31 * result + (int) (initialCapacity ^ (initialCapacity >>> 32));
        result = 31 * result + (int) (periodInNanos ^ (periodInNanos >>> 32));
        return result;
    }

}