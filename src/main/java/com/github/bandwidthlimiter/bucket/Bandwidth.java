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

package com.github.bandwidthlimiter.bucket;

import java.io.Serializable;

import static com.github.bandwidthlimiter.bucket.BucketExceptions.*;

public final class Bandwidth implements Serializable {

    private final int indexInBucket;
    private final Capacity capacity;
    private final long initialCapacity;
    private final long period;
    private final boolean guaranteed;

    public Bandwidth() {
        indexInBucket = 0;
        capacity = null;
        initialCapacity = 0;
        period = 0;
        guaranteed = false;
    }

    public Bandwidth(int indexInBucket, Capacity capacity, long initialCapacity, long period, boolean guaranteed) {
        if (indexInBucket < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (capacity.getValue() <= 0) {
            throw nonPositiveCapacity(capacity.getValue());
        }
        if (initialCapacity < 0) {
            throw nonPositiveInitialCapacity(initialCapacity);
        }
        if (initialCapacity > capacity.getValue()) {
            throw initialCapacityGreaterThanMaxCapacity(initialCapacity, capacity.getValue());
        }
        if (period <= 0) {
            throw nonPositivePeriod(period);
        }

        this.indexInBucket = indexInBucket;
        this.capacity = capacity;
        this.initialCapacity = initialCapacity;
        this.period = period;
        this.guaranteed = guaranteed;
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
        return capacity.getValue();
    }

    public int getIndexInBucket() {
        return indexInBucket;
    }

    public double getTimeUnitsPerToken() {
        return (double) period / (double) capacity.getValue();
    }

    public double getTokensPerTimeUnit() {
        return (double) capacity.getValue() / (double) period;
    }

    public static long getSmallestCapacityOfLimitedBandwidth(Bandwidth[] definitions) {
        long minCapacity = Long.MAX_VALUE;
        for (int i = 0; i < definitions.length; i++) {
            if (definitions[i].isLimited() && definitions[i].capacity.getValue() < minCapacity) {
                minCapacity = definitions[i].capacity.getValue();
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
                if (first.period < second.period && first.capacity.getValue() >= second.capacity.getValue()) {
                    throw hasOverlaps(first, second);
                } else if (first.period == second.period) {
                    throw hasOverlaps(first, second);
                } else if (first.period > second.period && first.capacity.getValue() <= second.capacity.getValue()) {
                    throw hasOverlaps(first, second);
                }
            }
        }
        if (guaranteedBandwidth != null) {
            for (Bandwidth bandwidth : bandwidths) {
                if (bandwidth.isLimited()) {
                    Bandwidth limited = bandwidth;
                    if (limited.getTokensPerTimeUnit() <= guaranteedBandwidth.getTokensPerTimeUnit()
                            || limited.getTimeUnitsPerToken() > guaranteedBandwidth.getTimeUnitsPerToken()) {
                        throw guarantedHasGreaterRateThanLimited(guaranteedBandwidth, limited);
                    }
                }
            }
        }
    }

}