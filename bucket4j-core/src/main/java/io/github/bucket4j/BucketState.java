/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j;

import java.io.Serializable;
import java.util.Arrays;

public class BucketState implements Serializable {

    private static final long serialVersionUID = 42L;

    private static final int LAST_REFILL_TIME_OFFSET = 0;

    final long[] stateData;

    BucketState(long[] stateData) {
        this.stateData = stateData;
    }

    public BucketState(BucketConfiguration configuration, long currentTimeNanos) {
        Bandwidth[] bandwidths = configuration.getBandwidths();
        long[] bandwidthsInitialTokens = configuration.getBandwidthsInitialTokens();

        this.stateData = new long[1 + bandwidths.length * 2];
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

    public BucketState copy() {
        return new BucketState(stateData.clone());
    }

    public void copyStateFrom(BucketState sourceState) {
        System.arraycopy(sourceState.stateData, 0, stateData, 0, stateData.length);
    }

    public static BucketState createInitialState(BucketConfiguration configuration, long currentTimeNanos) {
        return new BucketState(configuration, currentTimeNanos);
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
            resetBandwidth(bandwidthIndex, bandwidth.capacity);
        } else if (newSize < currentSize) {
            // arithmetic overflow happens. This mean that bucket reached Long.MAX_VALUE tokens.
            // just reset bandwidth state
            resetBandwidth(bandwidthIndex, bandwidth.capacity);
        } else {
            setCurrentSize(bandwidthIndex, newSize);
        }
    }

    private void consume(int bandwidth, long tokens) {
        stateData[1 + bandwidth * 2] -= tokens;
    }

    private void refill(int bandwidthIndex, Bandwidth bandwidth, long previousRefillNanos, long currentTimeNanos) {
        final long capacity = bandwidth.capacity;
        final long refillPeriodNanos = bandwidth.refill.getPeriodNanos();
        final long refillTokens = bandwidth.refill.getTokens();
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

    private long delayNanosAfterWillBePossibleToConsume(int bandwidthIndex, Bandwidth bandwidth, long tokens) {
        long currentSize = getCurrentSize(bandwidthIndex);
        if (tokens <= currentSize) {
            return 0;
        }
        long deficit = tokens - currentSize;
        long refillPeriodNanos = bandwidth.refill.getPeriodNanos();
        long refillPeriodTokens = bandwidth.refill.getTokens();

        long divided = multiplyExactOrReturnMaxValue(refillPeriodNanos, deficit);
        if (divided == Long.MAX_VALUE) {
            // arithmetic overflow happens.
            // there is no sense to stay in integer arithmetic when having deal with so big numbers
            return (long)((double) deficit / (double)refillPeriodTokens * (double)refillPeriodNanos);
        } else {
            return divided / refillPeriodTokens;
        }
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
