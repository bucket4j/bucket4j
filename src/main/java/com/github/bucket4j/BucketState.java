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

import java.io.Serializable;
import java.util.Arrays;

public class BucketState implements Serializable {

    private static final int LAST_REFILL_TIME_OFFSET = 0;

    private final long[] stateData;

    private BucketState(long[] stateData) {
        this.stateData = stateData;
    }

    public BucketState(BucketConfiguration configuration) {
        Bandwidth[] limits = configuration.getLimitedBandwidths();
        long[] limitsInitialTokens = configuration.getLimitedBandwidthsInitialTokens();
        Bandwidth guarantee = configuration.getGuaranteedBandwidth();
        long guarateeInitialTokens = configuration.getGuaranteedBandwidthInitialTokens();
        int totalBandwidthCount = limits.length + (guarantee == null? 0 : 1);

        this.stateData = new long[1 + totalBandwidthCount * 2];
        long currentTimeNanos = configuration.getTimeMeter().currentTimeNanos();
        for(int i = 0; i < limits.length; i++) {
            Bandwidth bandwidth = limits[i];
            long initialTokens = limitsInitialTokens[i];
            if (initialTokens == BucketConfiguration.INITIAL_TOKENS_UNSPECIFIED) {
                initialTokens = bandwidth.getCapacity().getValue(currentTimeNanos);
            }
            setCurrentSize(i, initialTokens);
        }
        if (guarantee != null) {
            if (guarateeInitialTokens == BucketConfiguration.INITIAL_TOKENS_UNSPECIFIED) {
                guarateeInitialTokens = guarantee.getCapacity().getValue(currentTimeNanos);
            }
            setCurrentSize(limits.length, guarateeInitialTokens);
        }
        setLastRefillTimeNanos(currentTimeNanos);
    }

    @Override
    public BucketState clone() {
        return new BucketState(stateData.clone());
    }

    public void copyStateFrom(BucketState sourceState) {
        System.arraycopy(sourceState.stateData, 0, stateData, 0, stateData.length);
    }

    public static BucketState createInitialState(BucketConfiguration configuration) {
        return new BucketState(configuration);
    }

    public long getAvailableTokens(Bandwidth[] limits, Bandwidth guarantee) {
        long availableByLimitation = Long.MAX_VALUE;
        long availableByGuarantee = guarantee == null? 0 : getCurrentSize(limits.length);
        for (int i = 0; i < limits.length; i++) {
            availableByLimitation = Math.min(availableByLimitation, getCurrentSize(i));
        }
        return Math.max(availableByLimitation, availableByGuarantee);
    }

    public void consume(Bandwidth[] limits, Bandwidth guarantee, long toConsume) {
        for (int i = 0; i < limits.length; i++) {
            consume(i, toConsume);
        }
        if (guarantee != null) {
            consume(limits.length, toConsume);
        }
    }

    public long delayNanosAfterWillBePossibleToConsume(Bandwidth[] limits, Bandwidth guarantee, long currentTime, long tokensToConsume) {
        long delayAfterWillBePossibleToConsumeGuaranteed = Long.MAX_VALUE;
        if (guarantee != null) {
            delayAfterWillBePossibleToConsumeGuaranteed = delayNanosAfterWillBePossibleToConsume(limits.length, guarantee, currentTime, tokensToConsume);
            if (delayAfterWillBePossibleToConsumeGuaranteed == 0) {
                return 0;
            }
        }

        long delayAfterWillBePossibleToConsumeLimited = 0;
        for (int i = 0; i < limits.length; i++) {
            Bandwidth limit = limits[i];
            long delay = delayNanosAfterWillBePossibleToConsume(i, limit, currentTime, tokensToConsume);
            if (delay > delayAfterWillBePossibleToConsumeLimited) {
                delayAfterWillBePossibleToConsumeLimited = delay;
            }
        }
        return Math.min(delayAfterWillBePossibleToConsumeLimited, delayAfterWillBePossibleToConsumeGuaranteed);
    }

    public void refillAllBandwidth(Bandwidth[] limits, Bandwidth guarantee, long currentTimeNanos) {
        long lastRefillTimeNanos = getLastRefillTimeNanos();
        if (currentTimeNanos <= lastRefillTimeNanos) {
            return;
        }
        for (int i = 0; i < limits.length; i++) {
            refill(i, limits[i], lastRefillTimeNanos, currentTimeNanos);
        }
        if (guarantee != null) {
            refill(limits.length, guarantee, lastRefillTimeNanos, currentTimeNanos);
        }
        setLastRefillTimeNanos(currentTimeNanos);
    }

    private void consume(int bandwidth, long tokens) {
        long currentSize = getCurrentSize(bandwidth);
        long newSize = currentSize - tokens;
        if (newSize < 0) {
            setCurrentSize(bandwidth, 0L);
            setRoundingError(bandwidth, 0L);
        } else {
            setCurrentSize(bandwidth, newSize);
        }
    }

    private void refill(int bandwidthIndex, Bandwidth bandwidth, long previousRefillNanos, long currentTimeNanos) {
        final long capacity = bandwidth.getCapacity().getValue(currentTimeNanos);
        long currentSize = getCurrentSize(bandwidthIndex);

        if (currentSize >= capacity) {
            setCurrentSize(bandwidthIndex, capacity);
            setRoundingError(bandwidthIndex, 0L);
            return;
        }

        long durationSinceLastRefillNanos = currentTimeNanos - previousRefillNanos;

        long refillPeriod = bandwidth.getRefill().getPeriodNanos();
        long refillTokens = bandwidth.getRefill().getTokens();
        long roundingError = getRoundingError(bandwidthIndex);
        long divided = refillTokens * durationSinceLastRefillNanos + roundingError;
        long calculatedRefill = divided / refillPeriod;
        if (calculatedRefill == 0) {
            setRoundingError(bandwidthIndex, divided);
            return;
        }

        long newSize = currentSize + calculatedRefill;
        if (newSize >= capacity) {
            setCurrentSize(bandwidthIndex, capacity);
            setRoundingError(bandwidthIndex, 0);
            return;
        }

        roundingError = divided % refillPeriod;
        setCurrentSize(bandwidthIndex, newSize);
        setRoundingError(bandwidthIndex, roundingError);
    }

    private long delayNanosAfterWillBePossibleToConsume(int bandwidthIndex, Bandwidth bandwidth,  long currentTimeNanos, long tokens) {
        long currentSize = getCurrentSize(bandwidthIndex);
        if (tokens <= currentSize) {
            return 0;
        }
        final long maxCapacity = bandwidth.getCapacity().getValue(currentTimeNanos);
        if (tokens > maxCapacity) {
            return Long.MAX_VALUE;
        }
        long deficit = tokens - currentSize;
        long periodNanos = bandwidth.getRefill().getPeriodNanos();
        return periodNanos * deficit / bandwidth.getRefill().getTokens();
    }

    long getCurrentSize(int bandwidth) {
        return stateData[1 + bandwidth * 2];
    }

    long getRoundingError(int bandwidth) {
        return stateData[2 + bandwidth * 2];
    }

    private void setCurrentSize(int bandwidth, long currentSize) {
        stateData[1 + bandwidth * 2] = currentSize;
    }

    private void setRoundingError(int bandwidth, long roundingError) {
        stateData[2 + bandwidth * 2] = roundingError;
    }

    private long getLastRefillTimeNanos() {
        return stateData[LAST_REFILL_TIME_OFFSET];
    }

    private void setLastRefillTimeNanos(long nanos) {
        stateData[LAST_REFILL_TIME_OFFSET] = nanos;
    }

    @Override
    public String toString() {
        return "BucketState{" +
                ", bandwidthStates=" + Arrays.toString(stateData) +
                '}';
    }

}
