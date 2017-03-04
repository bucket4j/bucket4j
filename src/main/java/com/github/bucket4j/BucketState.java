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
        this.stateData = new long[1 + bandwidths.length];
        for(int i = 0; i < bandwidths.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            setCurrentSize(i, bandwidth.getInitialTokens());
        }
        long currentTimeNanos = configuration.getTimeMeter().currentTimeNanos();
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
            Bandwidth bandwidth = bandwidths[i];
            BandwidthState bandwidthState = bandwidthStates[i];
            bandwidth.consume(bandwidthState, toConsume);
        }
    }

    public long delayNanosAfterWillBePossibleToConsume(Bandwidth[] bandwidths, long currentTime, long tokensToConsume) {
        long delayAfterWillBePossibleToConsumeLimited = 0;
        long delayAfterWillBePossibleToConsumeGuaranteed = Long.MAX_VALUE;
        for (int i = 0; i < bandwidthStates.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            BandwidthState bandwidthState = bandwidthStates[i];
            long delay = bandwidth.delayNanosAfterWillBePossibleToConsume(bandwidthState, currentTime, tokensToConsume);
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

    public void refill(Bandwidth[] bandwidths, long currentTimeNanos) {
        long lastRefillTimeNanos = stateData[0];
        if (lastRefillTimeNanos == currentTimeNanos) {
            return;
        }
        for (int i = 0; i < bandwidths.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            BandwidthState bandwidthState = bandwidthStates[i];
            bandwidth.refill(bandwidthState, lastRefillTimeNanos, currentTimeNanos);
        }
        lastRefillTimeNanos = currentTimeNanos;
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

    public void refill(BandwidthState state, long previousRefillNanos, long currentTimeNanos) {
        final long maxCapacity = capacity.getCapacity(currentTimeNanos);
        long currentSize = state.getCurrentSize();

        if (currentSize >= maxCapacity) {
            state.setCurrentSize(maxCapacity);
            state.setRoundingError(0);
            return;
        }

        long durationSinceLastRefillNanos = currentTimeNanos - previousRefillNanos;

        if (durationSinceLastRefillNanos > periodNanos) {
            state.setCurrentSize(maxCapacity);
            state.setRoundingError(0);
            return;
        }

        long roundingError = state.getRoundingError();
        long divided = maxCapacity * durationSinceLastRefillNanos + roundingError;
        long calculatedRefill = divided / periodNanos;
        if (calculatedRefill == 0) {
            roundingError = divided % periodNanos;
            state.setRoundingError(roundingError);
            return;
        }

        long newSize = currentSize + calculatedRefill;
        if (newSize >= maxCapacity) {
            state.setCurrentSize(maxCapacity);
            state.setRoundingError(0);
            return;
        }

        roundingError = divided % periodNanos;
        state.setCurrentSize(newSize);
        state.setRoundingError(roundingError);
    }

    public long delayNanosAfterWillBePossibleToConsume(int bandwidthIndex, Bandwidth bandwidth,  long currentTimeNanos, long tokens) {
        long currentSize = getCurrentSize(bandwidthIndex);
        if (tokens <= currentSize) {
            return 0;
        }
        final long maxCapacity = bandwidth.getCapacity().getCurrent(currentTimeNanos);
        if (tokens > maxCapacity) {
            return Long.MAX_VALUE;
        }
        long deficit = tokens - currentSize;
        return periodNanos * deficit / maxCapacity;
    }

    public void setInitialState(Bandwidth bandwidth, long currentSize) {
        return new BandwidthState(currentSize, 0L);
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
