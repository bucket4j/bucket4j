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

import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.*;
import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.guarantedHasGreaterRateThanLimited;
import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.hasOverlaps;
import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.restrictionsNotSpecified;

public final class Bandwidth {

    private final int indexInBucket;
    private final long capacity;
    private final long initialCapacity;
    private final long period;
    private final double tokensPerTimeUnit;
    private final double nanosecondsToGenerateOneToken;
    private final boolean guaranteed;

    public Bandwidth(int indexInBucket, long capacity, long period) {
        this(indexInBucket, capacity, period, false);
    }

    public Bandwidth(int indexInBucket, long capacity, long period, boolean guaranted) {
        this(indexInBucket, capacity, capacity, period, guaranted);
    }

    public Bandwidth(int indexInBucket, long capacity, long initialCapacity, long period, boolean guaranted) {
        if (indexInBucket < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (capacity <= 0) {
            throw nonPositiveCapacity(capacity);
        }
        if (initialCapacity < 0) {
            throw nonPositiveInitialCapacity(initialCapacity);
        }
        if (initialCapacity > capacity) {
            throw initialCapacityGreaterThanMaxCapacity(initialCapacity, capacity);
        }
        if (period <= 0) {
            throw nonPositivePeriod(period);
        }

        this.indexInBucket = indexInBucket;
        this.capacity = capacity;
        this.initialCapacity = initialCapacity;
        this.period = period;
        this.tokensPerTimeUnit = (double) capacity / (double) period;
        this.nanosecondsToGenerateOneToken = (double) period / (double) capacity;
        this.guaranteed = guaranted;
    }

    public boolean isGuaranteed() {
        return guaranteed;
    }

    public boolean isLimited() {
        return !guaranteed;
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

    public int getIndexInBucket() {
        return indexInBucket;
    }

    public static long getSmallestCapacityOfLimitedBandwidth(Bandwidth[] definitions) {
        long minCapacity = Long.MAX_VALUE;
        for (int i = 0; i < definitions.length; i++) {
            if (definitions[i].isLimited() && definitions[i].capacity < minCapacity) {
                minCapacity = definitions[i].capacity;
            }
        }
        return minCapacity;
    }

    public static void checkBandwidths(Bandwidth[] bandwidths) {
        int countOfLimitedBandwidth = 0;
        int countOfGuaranteedBandwidth = 0;
        Bandwidth guaranteedBandwidth = null;

        for (Bandwidth bandwidth: bandwidths) {
            if (bandwidth.isLimited()) {
                countOfLimitedBandwidth++;
            } else {
                guaranteedBandwidth = bandwidth;
                countOfGuaranteedBandwidth++;
            }
        }

        if (countOfLimitedBandwidth == 0) {
            throw restrictionsNotSpecified();
        }

        if (countOfGuaranteedBandwidth > 1) {
            throw onlyOneGuarantedBandwidthSupported();
        }

        for (int i = 0; i < bandwidths.length - 1; i++) {
            Bandwidth first = bandwidths[i];
            if (first.isGuaranteed()) {
                continue;
            }
            for (int j = i + 1; j < bandwidths.length; j++) {
                Bandwidth second = bandwidths[j];
                if (second.isGuaranteed()) {
                    continue;
                }
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
            for (Bandwidth bandwidth : bandwidths) {
                if (bandwidth.isLimited()) {
                    Bandwidth limited = bandwidth;
                    if (limited.tokensPerTimeUnit <= guaranteedBandwidth.tokensPerTimeUnit
                            || limited.nanosecondsToGenerateOneToken > guaranteedBandwidth.nanosecondsToGenerateOneToken) {
                        throw guarantedHasGreaterRateThanLimited(guaranteedBandwidth, limited);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Bandwidth{" +
                "capacity=" + capacity +
                ", initialCapacity=" + initialCapacity +
                ", period=" + period +
                ", tokensPerTimeUnit=" + tokensPerTimeUnit +
                ", nanosecondsToGenerateOneToken=" + nanosecondsToGenerateOneToken +
                ", guaranteed=" + guaranteed +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bandwidth bandwidth = (Bandwidth) o;

        if (capacity != bandwidth.capacity) return false;
        if (initialCapacity != bandwidth.initialCapacity) return false;
        if (period != bandwidth.period) return false;
        if (Double.compare(bandwidth.tokensPerTimeUnit, tokensPerTimeUnit) != 0) return false;
        if (Double.compare(bandwidth.nanosecondsToGenerateOneToken, nanosecondsToGenerateOneToken) != 0) return false;
        return guaranteed == bandwidth.guaranteed;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) (capacity ^ (capacity >>> 32));
        result = 31 * result + (int) (initialCapacity ^ (initialCapacity >>> 32));
        result = 31 * result + (int) (period ^ (period >>> 32));
        temp = Double.doubleToLongBits(tokensPerTimeUnit);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(nanosecondsToGenerateOneToken);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (guaranteed ? 1 : 0);
        return result;
    }
}