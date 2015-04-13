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

import static com.github.bandwidthlimiter.bucket.BucketExceptions.nonPositiveInitialCapacity;
import static com.github.bandwidthlimiter.bucket.BucketExceptions.nullBandwidthAdjuster;

public class Bandwidth implements Serializable {

    public static final int CURRENT_SIZE_OFFSET = 0;
    public static final int MAX_CAPACITY_OFFSET = CURRENT_SIZE_OFFSET + 1;
    public static final int ROUNDING_ERROR_OFFSET = MAX_CAPACITY_OFFSET + 1;

    private final long initialCapacity;
    private final long period;
    private final boolean guaranteed;
    private final BandwidthAdjuster adjuster;
    private final int stateOffset;

    public Bandwidth(int stateOffset, BandwidthAdjuster adjuster, long initialCapacity, long period, boolean guaranteed) {
        this.stateOffset = stateOffset;
        if (adjuster == null) {
            throw nullBandwidthAdjuster();
        }

        this.adjuster = adjuster;
        this.initialCapacity = initialCapacity;
        this.period = period;
        this.guaranteed = guaranteed;

        if (initialCapacity < 0) {
            throw nonPositiveInitialCapacity(initialCapacity);
        }
    }

    public boolean isGuaranteed() {
        return guaranteed;
    }

    public boolean isLimited() {
        return !guaranteed;
    }

    public int sizeOfState() {
        return 3;
    }

    public void setupInitialState(BucketState state, long currentTime) {
        setCurrentSize(state, initialCapacity);
        setMaxCapacity(state, adjuster.getCapacity());
        setRoundingError(state, 0);
    }

    public void consume(BucketState state, long toConsume) {
        long currentSize = getCurrentSize(state);
        currentSize = Math.max(0, currentSize - toConsume);
        setCurrentSize(state, currentSize);
    }

    public void refill(BucketState state, long currentTime) {
        long previousRefillTime = state.getRefillTime();
        final long maxCapacity = adjuster.getCapacity();
        long currentSize = getCurrentSize(state);

        setMaxCapacity(state, maxCapacity);
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

    public long timeRequiredToRefill(BucketState state, long numTokens) {
        return period * numTokens / getMaxCapacity(state);
    }

    public long getCurrentSize(BucketState state) {
        return state.getValue(stateOffset + CURRENT_SIZE_OFFSET);
    }

    public void setCurrentSize(BucketState state, long currentSize) {
        state.setValue(stateOffset + CURRENT_SIZE_OFFSET, currentSize);
    }

    public long getMaxCapacity(BucketState state) {
        return state.getValue(stateOffset + MAX_CAPACITY_OFFSET);
    }

    public void setMaxCapacity(BucketState state, long maxCapacity) {
        state.setValue(stateOffset + MAX_CAPACITY_OFFSET, maxCapacity);
    }

    public long getRoundingError(BucketState state) {
        return state.getValue(stateOffset + ROUNDING_ERROR_OFFSET);
    }

    public void setRoundingError(BucketState state, long roundingError) {
        state.setValue(stateOffset + ROUNDING_ERROR_OFFSET, roundingError);
    }

}