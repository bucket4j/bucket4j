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
        Bandwidth[] bandwidths = configuration.getBandwidths();
        long[] bandwidthsInitialTokens = configuration.getBandwidthsInitialTokens();

        this.stateData = new long[1 + bandwidths.length * 2];
        long currentTimeNanos = configuration.getTimeMeter().currentTimeNanos();
        for(int i = 0; i < bandwidths.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            long initialTokens = bandwidthsInitialTokens[i];
            if (initialTokens == BucketConfiguration.INITIAL_TOKENS_UNSPECIFIED) {
                initialTokens = bandwidth.capacity;
            }
            setCurrentSize(i, initialTokens);
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

    public long getAvailableTokens(Bandwidth[] bandwidths) {
        long availableTokens = getCurrentSize(0);
        for (int i = 1; i < bandwidths.length; i++) {
            availableTokens = Math.min(availableTokens, getCurrentSize(i));
        }
        return availableTokens;
    }

    public void consume(Bandwidth[] bandwidths, long toConsume) {
        for (int i = 0; i < bandwidths.length; i++) {
            consume(i, toConsume);
        }
    }

    public void reserve(Bandwidth[] bandwidths, long tokensToReserve) {
        for (int i = 0; i < bandwidths.length; i++) {
            reserve(i, tokensToReserve);
        }
    }

    public long delayNanosAfterWillBePossibleToConsume(Bandwidth[] bandwidths, long tokensToConsume) {
        long delayAfterWillBePossibleToConsume = delayNanosAfterWillBePossibleToConsume(0, bandwidths[0], tokensToConsume);
        for (int i = 1; i < bandwidths.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            long delay = delayNanosAfterWillBePossibleToConsume(i, bandwidth, tokensToConsume);
            delayAfterWillBePossibleToConsume = Math.max(delayAfterWillBePossibleToConsume, delay);
            if (delay > delayAfterWillBePossibleToConsume) {
                delayAfterWillBePossibleToConsume = delay;
            }
        }
        return delayAfterWillBePossibleToConsume;
    }

    public void refillAllBandwidth(Bandwidth[] limits, long currentTimeNanos) {
        long lastRefillTimeNanos = getLastRefillTimeNanos();
        if (currentTimeNanos <= lastRefillTimeNanos) {
            return;
        }
        for (int i = 0; i < limits.length; i++) {
            refill(i, limits[i], lastRefillTimeNanos, currentTimeNanos);
        }
        setLastRefillTimeNanos(currentTimeNanos);
    }

    public void addTokens(Bandwidth[] limits, long tokensToAdd) {
        for (int i = 0; i < limits.length; i++) {
            addTokens(i, limits[i], tokensToAdd);
        }
    }

    private void addTokens(int bandwidthIndex, Bandwidth bandwidth, long tokensToAdd) {
        long currentSize = getCurrentSize(bandwidthIndex);
        long newSize = currentSize + tokensToAdd;
        if (newSize >= bandwidth.capacity) {
            setCurrentSize(bandwidthIndex, bandwidth.capacity);
            setRoundingError(bandwidthIndex, 0L);
        } else {
            setCurrentSize(bandwidthIndex, newSize);
        }
    }

    private void consume(int bandwidth, long tokens) {
        stateData[1 + bandwidth * 2] -= tokens;
    }

    private void reserve(int bandwidth, long tokens) {
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
        final long capacity = bandwidth.capacity;
        long currentSize = getCurrentSize(bandwidthIndex);

        if (currentSize >= capacity) {
            setCurrentSize(bandwidthIndex, capacity);
            setRoundingError(bandwidthIndex, 0L);
            return;
        }

        long durationSinceLastRefillNanos = currentTimeNanos - previousRefillNanos;

        long refillPeriod = bandwidth.refill.getPeriodNanos();
        long refillTokens = bandwidth.refill.getTokens();
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

    private long delayNanosAfterWillBePossibleToConsume(int bandwidthIndex, Bandwidth bandwidth, long tokens) {
        long currentSize = getCurrentSize(bandwidthIndex);
        if (tokens <= currentSize) {
            return 0;
        }
        long deficit = tokens - currentSize;
        long periodNanos = bandwidth.refill.getPeriodNanos();
        return periodNanos * deficit / bandwidth.refill.getTokens();
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
