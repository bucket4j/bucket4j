/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.core_algorithms;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;

import java.util.Arrays;

public class BucketState64BitsInteger implements BucketState {

    private static final long serialVersionUID = 42L;

    private static final int BANDWIDTH_SIZE = 3;

    final long[] stateData;

    BucketState64BitsInteger(long[] stateData) {
        this.stateData = stateData;
    }

    public BucketState64BitsInteger(BucketConfiguration configuration, long currentTimeNanos) {
        Bandwidth[] bandwidths = configuration.getBandwidths();

        this.stateData = new long[bandwidths.length * 3];
        for(int i = 0; i < bandwidths.length; i++) {
            setCurrentSize(i, bandwidths[i].getInitialTokens());
            setLastRefillTimeNanos(i, currentTimeNanos);
        }
    }

    @Override
    public BucketState copy() {
        return new BucketState64BitsInteger(stateData.clone());
    }

    @Override
    public void copyStateFrom(BucketState sourceState) {
        BucketState64BitsInteger sourceState64BitsInteger = (BucketState64BitsInteger) sourceState;
        System.arraycopy(sourceState64BitsInteger.stateData, 0, stateData, 0, stateData.length);
    }

    @Override
    public long getAvailableTokens(Bandwidth[] bandwidths) {
        long availableTokens = getCurrentSize(0);
        for (int i = 1; i < bandwidths.length; i++) {
            availableTokens = Math.min(availableTokens, getCurrentSize(i));
        }
        return availableTokens;
    }

    @Override
    public void consume(Bandwidth[] bandwidths, long toConsume) {
        for (int i = 0; i < bandwidths.length; i++) {
            consume(i, toConsume);
        }
    }

    @Override
    public long calculateDelayNanosAfterWillBePossibleToConsume(Bandwidth[] bandwidths, long tokensToConsume, long currentTimeNanos) {
        long delayAfterWillBePossibleToConsume = calculateDelayNanosAfterWillBePossibleToConsume(0, bandwidths[0], tokensToConsume, currentTimeNanos);
        for (int i = 1; i < bandwidths.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            long delay = calculateDelayNanosAfterWillBePossibleToConsume(i, bandwidth, tokensToConsume, currentTimeNanos);
            delayAfterWillBePossibleToConsume = Math.max(delayAfterWillBePossibleToConsume, delay);
            if (delay > delayAfterWillBePossibleToConsume) {
                delayAfterWillBePossibleToConsume = delay;
            }
        }
        return delayAfterWillBePossibleToConsume;
    }

    @Override
    public void refillAllBandwidth(Bandwidth[] limits, long currentTimeNanos) {
        for (int i = 0; i < limits.length; i++) {
            refill(i, limits[i], currentTimeNanos);
        }
    }

    @Override
    public void addTokens(Bandwidth[] limits, long tokensToAdd) {
        for (int i = 0; i < limits.length; i++) {
            addTokens(i, limits[i], tokensToAdd);
        }
    }

    private void addTokens(int bandwidthIndex, Bandwidth bandwidth, long tokensToAdd) {
        long currentSize = getCurrentSize(bandwidthIndex);
        long newSize = currentSize + tokensToAdd;
        if (newSize >= bandwidth.getCapacity()) {
            resetBandwidth(bandwidthIndex, bandwidth.getCapacity());
        } else if (newSize < currentSize) {
            // arithmetic overflow happens. This mean that bucket reached Long.MAX_VALUE tokens.
            // just reset bandwidth state
            resetBandwidth(bandwidthIndex, bandwidth.getCapacity());
        } else {
            setCurrentSize(bandwidthIndex, newSize);
        }
    }

    private void refill(int bandwidthIndex, Bandwidth bandwidth, long currentTimeNanos) {
        long previousRefillNanos = getLastRefillTimeNanos(bandwidthIndex);
        if (currentTimeNanos <= previousRefillNanos) {
            return;
        }

        if (bandwidth.isRefillIntervally()) {
            long incompleteIntervalCorrection = (currentTimeNanos - previousRefillNanos) % bandwidth.getRefillPeriodNanos();
            currentTimeNanos -= incompleteIntervalCorrection;
        }
        if (currentTimeNanos <= previousRefillNanos) {
            return;
        } else {
            setLastRefillTimeNanos(bandwidthIndex, currentTimeNanos);
        }

        final long capacity = bandwidth.getCapacity();
        final long refillPeriodNanos = bandwidth.getRefillPeriodNanos();
        final long refillTokens = bandwidth.getRefillTokens();
        final long currentSize = getCurrentSize(bandwidthIndex);

        long durationSinceLastRefillNanos = currentTimeNanos - previousRefillNanos;
        long newSize = currentSize;

        if (durationSinceLastRefillNanos > refillPeriodNanos) {
            long elapsedPeriods = durationSinceLastRefillNanos / refillPeriodNanos;
            long calculatedRefill = elapsedPeriods * refillTokens;
            newSize += calculatedRefill;
            if (newSize > capacity) {
                resetBandwidth(bandwidthIndex, capacity);
                return;
            }
            if (newSize < currentSize) {
                // arithmetic overflow happens. This mean that tokens reached Long.MAX_VALUE tokens.
                // just reset bandwidth state
                resetBandwidth(bandwidthIndex, capacity);
                return;
            }
            durationSinceLastRefillNanos %= refillPeriodNanos;
        }


        long roundingError = getRoundingError(bandwidthIndex);
        long dividedWithoutError = multiplyExactOrReturnMaxValue(refillTokens, durationSinceLastRefillNanos);
        long divided = dividedWithoutError + roundingError;
        if (divided < 0 || dividedWithoutError == Long.MAX_VALUE) {
            // arithmetic overflow happens.
            // there is no sense to stay in integer arithmetic when having deal with so big numbers
            long calculatedRefill = (long) ((double) durationSinceLastRefillNanos / (double) refillPeriodNanos * (double) refillTokens);
            newSize += calculatedRefill;
            roundingError = 0;
        } else {
            long calculatedRefill = divided / refillPeriodNanos;
            if (calculatedRefill == 0) {
                roundingError = divided;
            } else {
                newSize += calculatedRefill;
                roundingError = divided % refillPeriodNanos;
            }
        }

        if (newSize >= capacity) {
            resetBandwidth(bandwidthIndex, capacity);
            return;
        }
        if (newSize < currentSize) {
            // arithmetic overflow happens. This mean that bucket reached Long.MAX_VALUE tokens.
            // just reset bandwidth state
            resetBandwidth(bandwidthIndex, capacity);
            return;
        }
        setCurrentSize(bandwidthIndex, newSize);
        setRoundingError(bandwidthIndex, roundingError);
    }

    private void resetBandwidth(int bandwidthIndex, long capacity) {
        setCurrentSize(bandwidthIndex, capacity);
        setRoundingError(bandwidthIndex, 0);
    }

    private long calculateDelayNanosAfterWillBePossibleToConsume(int bandwidthIndex, Bandwidth bandwidth, long tokens, long currentTimeNanos) {
        long currentSize = getCurrentSize(bandwidthIndex);
        if (tokens <= currentSize) {
            return 0;
        }
        long deficit = tokens - currentSize;
        if (deficit <= 0) {
            // math overflow happen
            return Long.MAX_VALUE;
        }

        if (bandwidth.isRefillIntervally()) {
            return calculateDelayNanosAfterWillBePossibleToConsumeForIntervalBandwidth(bandwidthIndex, bandwidth, deficit, currentTimeNanos);
        } else {
            return calculateDelayNanosAfterWillBePossibleToConsumeForGreedyBandwidth(bandwidth, deficit);
        }
    }

    private long calculateDelayNanosAfterWillBePossibleToConsumeForGreedyBandwidth(Bandwidth bandwidth, long deficit) {
        long refillPeriodNanos = bandwidth.getRefillPeriodNanos();
        long refillPeriodTokens = bandwidth.getRefillTokens();
        long divided = multiplyExactOrReturnMaxValue(refillPeriodNanos, deficit);
        if (divided == Long.MAX_VALUE) {
            // math overflow happen.
            // there is no sense to stay in integer arithmetic when having deal with so big numbers
            return (long)((double) deficit / (double)refillPeriodTokens * (double)refillPeriodNanos);
        } else {
            return divided / refillPeriodTokens;
        }
    }

    private long calculateDelayNanosAfterWillBePossibleToConsumeForIntervalBandwidth(int bandwidthIndex, Bandwidth bandwidth, long deficit, long currentTimeNanos) {
        long refillPeriodNanos = bandwidth.getRefillPeriodNanos();
        long refillTokens = bandwidth.getRefillTokens();
        long previousRefillNanos = getLastRefillTimeNanos(bandwidthIndex);

        long timeOfNextRefillNanos = previousRefillNanos + refillPeriodNanos;
        long waitForNextRefillNanos = timeOfNextRefillNanos - currentTimeNanos;
        if (deficit <= refillTokens) {
            return waitForNextRefillNanos;
        }

        deficit -= refillTokens;
        if (deficit < refillTokens) {
            return waitForNextRefillNanos + refillPeriodNanos;
        }

        long deficitPeriods = deficit / refillTokens + (deficit % refillTokens == 0L? 0 : 1);
        long deficitNanos = multiplyExactOrReturnMaxValue(deficitPeriods, refillPeriodNanos);
        if (deficitNanos == Long.MAX_VALUE) {
            // math overflow happen
            return Long.MAX_VALUE;
        }
        deficitNanos += waitForNextRefillNanos;
        if (deficitNanos < 0) {
            // math overflow happen
            return Long.MAX_VALUE;
        }
        return deficitNanos;
    }

    private long getLastRefillTimeNanos(int bandwidth) {
        return stateData[bandwidth * BANDWIDTH_SIZE];
    }

    private void setLastRefillTimeNanos(int bandwidth, long nanos) {
        stateData[bandwidth * BANDWIDTH_SIZE] = nanos;
    }

    @Override
    public long getCurrentSize(int bandwidth) {
        return stateData[bandwidth * BANDWIDTH_SIZE + 1];
    }

    @Override
    public long getRoundingError(int bandwidth) {
        return stateData[bandwidth * BANDWIDTH_SIZE + 2];
    }

    private void setCurrentSize(int bandwidth, long currentSize) {
        stateData[bandwidth * BANDWIDTH_SIZE + 1] = currentSize;
    }

    private void consume(int bandwidth, long tokens) {
        stateData[bandwidth * BANDWIDTH_SIZE + 1] -= tokens;
    }

    private void setRoundingError(int bandwidth, long roundingError) {
        stateData[bandwidth * BANDWIDTH_SIZE + 2] = roundingError;
    }

    @Override
    public String toString() {
        return "BucketState{" +
                "bandwidthStates=" + Arrays.toString(stateData) +
                '}';
    }

    // just a copy of JDK method Math#multiplyExact,
    // but instead of throwing exception it returns Long.MAX_VALUE in case of overflow
    private static long multiplyExactOrReturnMaxValue(long x, long y) {
        long r = x * y;
        long ax = Math.abs(x);
        long ay = Math.abs(y);
        if (((ax | ay) >>> 31 != 0)) {
            // Some bits greater than 2^31 that might cause overflow
            // Check the result using the divide operator
            // and check for the special case of Long.MIN_VALUE * -1
            if (((y != 0) && (r / y != x)) || (x == Long.MIN_VALUE && y == -1)) {
                return Long.MAX_VALUE;
            }
        }
        return r;
    }

}
