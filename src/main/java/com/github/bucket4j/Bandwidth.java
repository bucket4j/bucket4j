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

public class Bandwidth implements Serializable {

    private final long initialCapacity;
    private final long periodNanos;
    private final boolean guaranteed;
    private final CapacityAdjuster adjuster;

    public Bandwidth(CapacityAdjuster adjuster, long initialCapacity, long periodNanos, boolean guaranteed) {
        this.adjuster = adjuster;
        this.initialCapacity = initialCapacity;
        this.periodNanos = periodNanos;
        this.guaranteed = guaranteed;
    }

    public boolean isGuaranteed() {
        return guaranteed;
    }

    public boolean isLimited() {
        return !guaranteed;
    }

    public BandwidthState createInitialState() {
        return BandwidthState.initialState(initialCapacity);
    }

    public void consume(BandwidthState state, long tokens) {
        long currentSize = state.getCurrentSize();
        long newSize = currentSize - tokens;
        if (newSize < 0) {
            state.setCurrentSize(0);
            state.setRoundingError(0);
        } else {
            state.setCurrentSize(newSize);
        }
    }

    public void refill(BandwidthState state, long previousRefillNanos, long currentTimeNanos) {
        final long maxCapacity = adjuster.getCapacity(currentTimeNanos);
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

    public long delayNanosAfterWillBePossibleToConsume(BandwidthState state, long currentTimeNanos, long tokens) {
        long currentSize = state.getCurrentSize();
        if (tokens <= currentSize) {
            return 0;
        }
        final long maxCapacity = getMaxCapacity(currentTimeNanos);
        if (tokens > maxCapacity) {
            return Long.MAX_VALUE;
        }
        long deficit = tokens - currentSize;
        return periodNanos * deficit / maxCapacity;
    }

    public long getMaxCapacity(long currentTime) {
        return adjuster.getCapacity(currentTime);
    }

    @Override
    public String toString() {
        return "Bandwidth{" +
                "initialCapacity=" + initialCapacity +
                ", periodNanos=" + periodNanos +
                ", guaranteed=" + guaranteed +
                ", adjuster=" + adjuster +
                '}';
    }

}