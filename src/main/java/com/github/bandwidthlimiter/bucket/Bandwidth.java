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

    public static final int CURRENT_SIZE = 0;
    public static final int MAX_CAPACITY = CURRENT_SIZE + 1;
    public static final int REFILL_TIME = MAX_CAPACITY + 1;

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
        setRefillTime(state, currentTime);
        setMaxCapacity(state, adjuster.getCapacity());
    }

    public void consume(BucketState state, long toConsume) {
        long currentSize = getCurrentSize(state);
        currentSize = Math.max(0, currentSize - toConsume);
        setCurrentSize(state, currentSize);
    }

    public void refill(BucketState state, long currentTime) {
        long previousRefillTime = getRefillTime(state);
        final long maxCapacity = adjuster.getCapacity();
        setMaxCapacity(state, maxCapacity);
        long durationSinceLastRefill = currentTime - previousRefillTime;

        if (durationSinceLastRefill > period) {
            setCurrentSize(state, maxCapacity);
            setRefillTime(state, currentTime);
            return;
        }

        long calculatedRefill = maxCapacity * durationSinceLastRefill / period;
        if (calculatedRefill == 0) {
            return;
        }

        long newSize = getCurrentSize(state) + calculatedRefill;
        if (newSize >= maxCapacity) {
            setCurrentSize(state, maxCapacity);
            setRefillTime(state, currentTime);
            return;
        }

        setCurrentSize(state, newSize);
        long effectiveDuration = calculatedRefill * period / maxCapacity;
        long roundingError = durationSinceLastRefill - effectiveDuration;
        long effectiveRefillTime = currentTime - roundingError;
        setRefillTime(state, effectiveRefillTime);
    }

    public long timeRequiredToRefill(long numTokens) {
        return period * numTokens / adjuster.getCapacity();
    }

    public long getCurrentSize(BucketState state) {
        return state.getValue(stateOffset + CURRENT_SIZE);
    }

    public void setCurrentSize(BucketState state, long currentSize) {
        state.setValue(stateOffset + CURRENT_SIZE, currentSize);
    }

    public long getMaxCapacity(BucketState state) {
        return state.getValue(stateOffset + MAX_CAPACITY);
    }

    public void setMaxCapacity(BucketState state, long maxCapacity) {
        state.setValue(stateOffset + MAX_CAPACITY, maxCapacity);
    }

    public long getRefillTime(BucketState state) {
        return state.getValue(stateOffset + REFILL_TIME);
    }

    public void setRefillTime(BucketState state, long refillTime) {
        state.setValue(stateOffset + REFILL_TIME, refillTime);
    }

}