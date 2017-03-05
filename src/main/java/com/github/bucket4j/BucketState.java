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
        Bandwidth[] bandwidths = configuration.getLimitedBandwidths();
        this.stateData = new long[1 + bandwidths.length];
        long currentTimeNanos = configuration.getTimeMeter().currentTimeNanos();
        for(int i = 0; i < bandwidths.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            Long initialTokens = bandwidth.getInitialTokens();
            if (initialTokens == null) {
                initialTokens = bandwidth.getCapacity().getValue(currentTimeNanos);
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
        long availableByLimitation = Long.MAX_VALUE;
        long availableByGuarantee = 0;
        for (int i = 0; i < bandwidths.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            if (bandwidth.isLimited()) {
                availableByLimitation = Math.min(availableByLimitation, getCurrentSize(i));
            } else {
                availableByGuarantee = getCurrentSize(i);
            }
        }
        return Math.max(availableByLimitation, availableByGuarantee);
    }

    public void consume(Bandwidth[] bandwidths, long toConsume) {
        for (int i = 0; i < bandwidths.length; i++) {
            consume(i, toConsume);
        }
    }

    public long delayNanosAfterWillBePossibleToConsume(Bandwidth[] bandwidths, long currentTime, long tokensToConsume) {
        long delayAfterWillBePossibleToConsumeLimited = 0;
        long delayAfterWillBePossibleToConsumeGuaranteed = Long.MAX_VALUE;
        for (int i = 0; i < bandwidths.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            long delay = delayNanosAfterWillBePossibleToConsume(i, bandwidth, currentTime, tokensToConsume);
            if (bandwidth.isGuaranteed()) {
                if (delay == 0) {
                    return 0;
                } else {
                    delayAfterWillBePossibleToConsumeGuaranteed = delay;
                }
                continue;
            }
            if (delay > delayAfterWillBePossibleToConsumeLimited) {
                delayAfterWillBePossibleToConsumeLimited = delay;
            }
        }
        return Math.min(delayAfterWillBePossibleToConsumeLimited, delayAfterWillBePossibleToConsumeGuaranteed);
    }

    public void consume(int bandwidth, long tokens) {
        long currentSize = getCurrentSize(bandwidth);
        long newSize = currentSize - tokens;
        if (newSize < 0) {
            setCurrentSize(bandwidth, 0L);
            setRoundingError(bandwidth, 0L);
        } else {
            setCurrentSize(bandwidth, newSize);
        }
    }

    public void refillAllBandwidth(Bandwidth[] bandwidths, long currentTimeNanos) {
        long lastRefillTimeNanos = getLastRefillTimeNanos();
        if (lastRefillTimeNanos == currentTimeNanos) {
            return;
        }
        for (int i = 0; i < bandwidths.length; i++) {
            refill(i, bandwidths[i], lastRefillTimeNanos, currentTimeNanos);
        }
        setLastRefillTimeNanos(currentTimeNanos);
    }

    public void refill(int bandwidthIndex, Bandwidth bandwidth, long previousRefillNanos, long currentTimeNanos) {
        final long maxCapacity = bandwidth.getCapacity().getValue(currentTimeNanos);
        long currentSize = getCurrentSize(bandwidthIndex);

        if (currentSize >= maxCapacity) {
            setCurrentSize(bandwidthIndex, maxCapacity);
            setRoundingError(bandwidthIndex, 0L);
            return;
        }

        long durationSinceLastRefillNanos = currentTimeNanos - previousRefillNanos;

        long periodNanos = bandwidth.getRefill().getPeriodNanos();
        if (durationSinceLastRefillNanos > periodNanos) {
            setCurrentSize(bandwidthIndex, maxCapacity);
            setRoundingError(bandwidthIndex, 0L);
            return;
        }

        long roundingError = getRoundingError(bandwidthIndex);
        long divided = bandwidth.getRefill().getTokens() * durationSinceLastRefillNanos + roundingError;
        long calculatedRefill = divided / periodNanos;
        if (calculatedRefill == 0) {
            roundingError = divided % periodNanos;
            setRoundingError(bandwidthIndex, roundingError);
            return;
        }

        long newSize = currentSize + calculatedRefill;
        if (newSize >= maxCapacity) {
            setCurrentSize(bandwidthIndex, maxCapacity);
            setRoundingError(bandwidthIndex, 0);
            return;
        }

        roundingError = divided % periodNanos;
        setCurrentSize(bandwidthIndex, newSize);
        setRoundingError(bandwidthIndex, roundingError);
    }

    public long delayNanosAfterWillBePossibleToConsume(int bandwidthIndex, Bandwidth bandwidth,  long currentTimeNanos, long tokens) {
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

    public long getCurrentSize(int bandwidth) {
        return stateData[1 + bandwidth * 2];
    }

    public long getRoundingError(int bandwidth) {
        return stateData[2 + bandwidth * 2];
    }

    public void setCurrentSize(int bandwidth, long currentSize) {
        stateData[1 + bandwidth * 2] = currentSize;
    }

    public void setRoundingError(int bandwidth, long roundingError) {
        stateData[2 + bandwidth * 2] = roundingError;
    }

    public long getLastRefillTimeNanos() {
        return stateData[LAST_REFILL_TIME_OFFSET];
    }

    public void setLastRefillTimeNanos(long nanos) {
        stateData[LAST_REFILL_TIME_OFFSET] = nanos;
    }

    @Override
    public String toString() {
        return "BucketState{" +
                ", bandwidthStates=" + Arrays.toString(stateData) +
                '}';
    }

}
