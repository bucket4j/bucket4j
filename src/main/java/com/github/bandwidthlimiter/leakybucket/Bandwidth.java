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

import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.guarantedHasGreaterRateThanLimited;
import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.hasOverlaps;
import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.restrictionsNotSpecified;

public final class Bandwidth {

    private final long capacity;
    private final long initialCapacity;
    private final long period;
    private final double tokensPerTimeUnit;
    private final double nanosecondsToGenerateOneToken;

    public Bandwidth(long capacity, long period) {
        this(capacity, capacity, period);
    }

    public Bandwidth(long capacity, long initialCapacity, long period) {
        if (capacity <= 0) {
            throw LeakyBucketExceptions.nonPositiveCapacity(capacity);
        }
        if (initialCapacity < 0) {
            throw LeakyBucketExceptions.nonPositiveInitialCapacity(initialCapacity);
        }
        if (initialCapacity > capacity) {
            throw LeakyBucketExceptions.initialCapacityGreaterThanMaxCapacity(initialCapacity, capacity);
        }
        if (period <= 0) {
            throw LeakyBucketExceptions.nonPositivePeriod(period);
        }

        this.capacity = capacity;
        this.initialCapacity = initialCapacity;
        this.period = period;
        this.tokensPerTimeUnit = (double) capacity / (double) period;
        this.nanosecondsToGenerateOneToken = (double) period / (double) capacity;
    }

    public long getPeriod() {
        return period;
    }

    public long getInitialCapacity() {
        return initialCapacity;
    }

    public long getMaxCapacity() {
        return capacity;
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
                if (first.period < second.period && first.capacity >= second.capacity) {
                    throw hasOverlaps(first, second);
                } else if (first.period == second.period) {
                    throw hasOverlaps(first, second);
                } else if (first.period > second.period && first.capacity <= second.capacity) {
                    throw hasOverlaps(first, second);
                }
            }
        }
        if (guaranteedBandwidth != null) {
            for (Bandwidth limited : limitedBandwidths) {
                if (limited.tokensPerTimeUnit <= guaranteedBandwidth.tokensPerTimeUnit
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
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bandwidth that = (Bandwidth) o;

        if (capacity != that.capacity) return false;
        if (initialCapacity != that.initialCapacity) return false;
        if (period != that.period) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (capacity ^ (capacity >>> 32));
        result = 31 * result + (int) (initialCapacity ^ (initialCapacity >>> 32));
        result = 31 * result + (int) (period ^ (period >>> 32));
        return result;
    }

}