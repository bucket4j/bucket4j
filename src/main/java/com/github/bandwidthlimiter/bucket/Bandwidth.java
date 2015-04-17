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

public class Bandwidth implements Serializable {

    public static final int CURRENT_SIZE_OFFSET = 0;
    public static final int ROUNDING_ERROR_OFFSET = CURRENT_SIZE_OFFSET + 1;

    private final long initialCapacity;
    private final long period;
    private final boolean guaranteed;
    private final BandwidthAdjuster adjuster;
    private final int stateOffset;

    public Bandwidth(int stateOffset, BandwidthAdjuster adjuster, long initialCapacity, long period, boolean guaranteed) {
        this.stateOffset = stateOffset;
        this.adjuster = adjuster;
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

    public int sizeOfState() {
        return 2;
    }

    public void setupInitialState(BucketState state) {
        setCurrentSize(state, initialCapacity);
        setRoundingError(state, 0);
    }

    public void consume(BucketState state, long toConsume) {
        long currentSize = getCurrentSize(state);
        long newSize = currentSize - toConsume;
        if (newSize < 0) {
            setCurrentSize(state, 0);
            setRoundingError(state, 0);
        } else {
            setCurrentSize(state, newSize);
        }
    }

    public void refill(BucketState state, long previousRefillTime, long currentTime) {
        final long maxCapacity = adjuster.getCapacity(currentTime);
        long currentSize = getCurrentSize(state);

        if (currentSize >= maxCapacity) {
            setCurrentSize(state, maxCapacity);
            setRoundingError(state, 0);
            return;
        }

        long durationSinceLastRefill = currentTime - previousRefillTime;

        if (durationSinceLastRefill > period) {
            setCurrentSize(state, maxCapacity);
            setRoundingError(state, 0);
            return;
        }

        long roundingError = getRoundingError(state);
        long divided = maxCapacity * durationSinceLastRefill + roundingError;
        long calculatedRefill = divided / period;
        if (calculatedRefill == 0) {
            roundingError = divided % period;
            setRoundingError(state, roundingError);
            return;
        }

        long newSize = currentSize + calculatedRefill;
        if (newSize >= maxCapacity) {
            setCurrentSize(state, maxCapacity);
            setRoundingError(state, 0);
            return;
        }

        roundingError = divided % period;
        setCurrentSize(state, newSize);
        setRoundingError(state, roundingError);
    }

    public long delayAfterWillBePossibleToConsume(BucketState state, long currentTime, long numTokens) {
        long currentSize = getCurrentSize(state);
        if (numTokens <= currentSize) {
            return 0;
        }
        final long maxCapacity = getMaxCapacity(currentTime);
        if (numTokens > maxCapacity) {
            return Long.MAX_VALUE;
        }
        long deficit = numTokens - currentSize;
        return period * deficit / maxCapacity;
    }

    public long getCurrentSize(BucketState state) {
        return state.getValue(stateOffset + CURRENT_SIZE_OFFSET);
    }

    public long getMaxCapacity(long currentTime) {
        return adjuster.getCapacity(currentTime);
    }

    private void setCurrentSize(BucketState state, long currentSize) {
        state.setValue(stateOffset + CURRENT_SIZE_OFFSET, currentSize);
    }

    private long getRoundingError(BucketState state) {
        return state.getValue(stateOffset + ROUNDING_ERROR_OFFSET);
    }

    private void setRoundingError(BucketState state, long roundingError) {
        state.setValue(stateOffset + ROUNDING_ERROR_OFFSET, roundingError);
    }

}