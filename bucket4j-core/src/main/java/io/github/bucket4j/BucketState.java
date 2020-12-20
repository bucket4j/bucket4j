/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.bucket4j;

import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.serialization.SerializationAdapter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class BucketState implements Serializable {

    private static final long serialVersionUID = 42L;

    private static final int BANDWIDTH_SIZE = 3;

    final long[] stateData;

    BucketState(long[] stateData) {
        this.stateData = stateData;
    }

    public BucketState(BucketConfiguration configuration, long currentTimeNanos) {
        Bandwidth[] bandwidths = configuration.getBandwidths();

        this.stateData = new long[bandwidths.length * 3];
        for(int i = 0; i < bandwidths.length; i++) {
            setCurrentSize(i, calculateInitialTokens(bandwidths[i], currentTimeNanos));
            setLastRefillTimeNanos(i, calculateLastRefillTimeNanos(bandwidths[i], currentTimeNanos));
        }
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

    public BucketState replaceConfiguration(BucketConfiguration previousConfiguration, BucketConfiguration newConfiguration,
            TokensMigrationMode tokensMigrationMode, long currentTimeNanos) {
        if (tokensMigrationMode == TokensMigrationMode.RESET) {
            return new BucketState(newConfiguration, currentTimeNanos);
        }

        boolean nullIdComparisonCanBeApplied = countOfBandwidthsWithNullIdentifiers(previousConfiguration) < 2
                && countOfBandwidthsWithNullIdentifiers(newConfiguration) < 2;

        Bandwidth[] previousBandwidths = previousConfiguration.getBandwidths();
        Bandwidth[] newBandwidths = newConfiguration.getBandwidths();

        BucketState newState = new BucketState(new long[newBandwidths.length * 3]);
        for (int newBandwidthIndex = 0; newBandwidthIndex < newBandwidths.length; newBandwidthIndex++) {
            Bandwidth newBandwidth = newBandwidths[newBandwidthIndex];
            Bandwidth previousBandwidth = null;
            int previousBandwidthIndex = -1;
            if (newBandwidth.getId() != null || nullIdComparisonCanBeApplied) {
                for (int j = 0; j < previousBandwidths.length; j++) {
                    if (Objects.equals(newBandwidth.getId(), previousBandwidths[j].getId()) ) {
                        previousBandwidth = previousBandwidths[j];
                        previousBandwidthIndex = j;
                        break;
                    }
                }
            }
            if (previousBandwidth == null) {
                newState.setCurrentSize(newBandwidthIndex, calculateInitialTokens(newBandwidth, currentTimeNanos));
                newState.setLastRefillTimeNanos(newBandwidthIndex, calculateLastRefillTimeNanos(newBandwidth, currentTimeNanos));
                continue;
            }

            if (tokensMigrationMode == TokensMigrationMode.AS_IS) {
                replaceBandwidthAsIs(newState, newBandwidthIndex, newBandwidth, previousBandwidthIndex, previousBandwidth, currentTimeNanos);
            } else if (tokensMigrationMode == TokensMigrationMode.PROPORTIONALLY) {
                replaceBandwidthProportional(newState, newBandwidthIndex, newBandwidth, previousBandwidthIndex, previousBandwidth, currentTimeNanos);
            } else {
                throw new IllegalStateException("Should never reach there");
            }
        }
        return newState;
    }

    private void replaceBandwidthAsIs(BucketState newState, int newBandwidthIndex, Bandwidth newBandwidth,
                      int previousBandwidthIndex, Bandwidth previousBandwidth, long currentTimeNanos) {
        long lastRefillTimeNanos = getLastRefillTimeNanos(previousBandwidthIndex);
        newState.setLastRefillTimeNanos(newBandwidthIndex, lastRefillTimeNanos);

        if (newBandwidth.isGready() && previousBandwidth.isGready()) {
            long currentSize = getCurrentSize(previousBandwidthIndex);
            long newSize = Math.min(newBandwidth.capacity, currentSize);
            newState.setCurrentSize(newBandwidthIndex, newSize);

            long roundingError = getRoundingError(previousBandwidthIndex);
            double roundingScale = (double) newBandwidth.refillPeriodNanos / (double) previousBandwidth.refillPeriodNanos;
            long newRoundingError = (long) roundingScale * roundingError;
            if (newRoundingError >= newBandwidth.refillPeriodNanos) {
                newRoundingError = newBandwidth.refillPeriodNanos - 1;
            }
            newState.setRoundingError(newBandwidthIndex, newRoundingError);
            return;
        }

        long currentSize = getCurrentSize(previousBandwidthIndex);
        long newSize = Math.min(newBandwidth.capacity, currentSize);
        newState.setCurrentSize(newBandwidthIndex, newSize);
    }

    private void replaceBandwidthProportional(BucketState newState, int newBandwidthIndex, Bandwidth newBandwidth, int previousBandwidthIndex, Bandwidth previousBandwidth, long currentTimeNanos) {
        newState.setLastRefillTimeNanos(newBandwidthIndex, getLastRefillTimeNanos(previousBandwidthIndex));
        long currentSize = getCurrentSize(previousBandwidthIndex);
        long roundingError = getRoundingError(previousBandwidthIndex);
        double realRoundedError = (double) roundingError / (double) previousBandwidth.refillPeriodNanos;
        double scale = (double) newBandwidth.capacity / (double) previousBandwidth.capacity;
        double realNewSize = ((double) currentSize + realRoundedError) * scale;
        long newSize = (long) realNewSize;

        if (newSize >= newBandwidth.capacity) {
            newState.setCurrentSize(newBandwidthIndex, newBandwidth.capacity);
            return;
        }
        if (newSize == Long.MIN_VALUE) {
            newState.setCurrentSize(newBandwidthIndex, Long.MIN_VALUE);
            return;
        }

        double restOfDivision = realNewSize - newSize;
        if (restOfDivision > 1.0d || restOfDivision < - 1.0d) {
            restOfDivision = realNewSize % 1;
        }
        if (restOfDivision == 0.0d) {
            newState.setCurrentSize(newBandwidthIndex, newSize);
            return;
        }

        if (realNewSize < 0) {
            newSize--;
            restOfDivision = restOfDivision + 1;
        }
        newState.setCurrentSize(newBandwidthIndex, newSize);
        if (newBandwidth.isGready()) {
            long newRoundingError = (long) (restOfDivision * newBandwidth.refillPeriodNanos);
            newState.setRoundingError(newBandwidthIndex, newRoundingError);
        }
    }

    private int countOfBandwidthsWithNullIdentifiers(BucketConfiguration configuration) {
        Bandwidth[] bandwidths = configuration.getBandwidths();
        int count = 0;
        for (int i = 0; i < bandwidths.length; i++) {
            if (bandwidths[i].getId() == null) {
                count++;
            }
        }
        return count;
    }

    public static void main(String[] args) {
        System.out.println(1.25d - 1);
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

    public long calculateDelayNanosAfterWillBePossibleToConsume(Bandwidth[] bandwidths, long tokensToConsume, long currentTimeNanos) {
        long delayAfterWillBePossibleToConsume = calculateDelayNanosAfterWillBePossibleToConsume(0, bandwidths[0], tokensToConsume, currentTimeNanos);
        for (int i = 1; i < bandwidths.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            long delay = calculateDelayNanosAfterWillBePossibleToConsume(i, bandwidth, tokensToConsume, currentTimeNanos);
            delayAfterWillBePossibleToConsume = Math.max(delayAfterWillBePossibleToConsume, delay);
        }
        return delayAfterWillBePossibleToConsume;
    }

    public void refillAllBandwidth(Bandwidth[] limits, long currentTimeNanos) {
        for (int i = 0; i < limits.length; i++) {
            refill(i, limits[i], currentTimeNanos);
        }
    }

    public void addTokens(Bandwidth[] limits, long tokensToAdd) {
        for (int i = 0; i < limits.length; i++) {
            addTokens(i, limits[i], tokensToAdd);
        }
    }

    private long calculateLastRefillTimeNanos(Bandwidth bandwidth, long currentTimeNanos) {
        if (!bandwidth.isIntervallyAligned()) {
            return currentTimeNanos;
        }
        return bandwidth.timeOfFirstRefillMillis * 1_000_000 - bandwidth.refillPeriodNanos;
    }

    private long calculateInitialTokens(Bandwidth bandwidth, long currentTimeNanos) {
        if (!bandwidth.useAdaptiveInitialTokens) {
            return bandwidth.initialTokens;
        }

        long timeOfFirstRefillNanos = bandwidth.timeOfFirstRefillMillis * 1_000_000;
        if (currentTimeNanos >= timeOfFirstRefillNanos) {
            return bandwidth.initialTokens;
        }

        long guaranteedBase = Math.max(0, bandwidth.capacity - bandwidth.refillTokens);
        long nanosBeforeFirstRefill = timeOfFirstRefillNanos - currentTimeNanos;
        if (multiplyExactOrReturnMaxValue(nanosBeforeFirstRefill, bandwidth.refillTokens) != Long.MAX_VALUE) {
            return Math.min(bandwidth.capacity, guaranteedBase + nanosBeforeFirstRefill * bandwidth.refillTokens / bandwidth.refillPeriodNanos);
        } else {
            // arithmetic overflow happens.
            // there is no sense to stay in integer arithmetic when having deal with so big numbers
            return Math.min(bandwidth.capacity, guaranteedBase + (long)((double)nanosBeforeFirstRefill * (double) bandwidth.refillTokens / (double) bandwidth.refillPeriodNanos));
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

    private void refill(int bandwidthIndex, Bandwidth bandwidth, long currentTimeNanos) {
        long previousRefillNanos = getLastRefillTimeNanos(bandwidthIndex);
        if (currentTimeNanos <= previousRefillNanos) {
            return;
        }

        if (bandwidth.refillIntervally) {
            long incompleteIntervalCorrection = (currentTimeNanos - previousRefillNanos) % bandwidth.refillPeriodNanos;
            currentTimeNanos -= incompleteIntervalCorrection;
        }
        if (currentTimeNanos <= previousRefillNanos) {
            return;
        } else {
            setLastRefillTimeNanos(bandwidthIndex, currentTimeNanos);
        }

        final long capacity = bandwidth.capacity;
        final long refillPeriodNanos = bandwidth.refillPeriodNanos;
        final long refillTokens = bandwidth.refillTokens;
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

        if (bandwidth.refillIntervally) {
            return calculateDelayNanosAfterWillBePossibleToConsumeForIntervalBandwidth(bandwidthIndex, bandwidth, deficit, currentTimeNanos);
        } else {
            return calculateDelayNanosAfterWillBePossibleToConsumeForGreedyBandwidth(bandwidthIndex, bandwidth, deficit);
        }
    }

    private long calculateDelayNanosAfterWillBePossibleToConsumeForGreedyBandwidth(int bandwidthIndex, Bandwidth bandwidth, long deficit) {
        long refillPeriodNanos = bandwidth.refillPeriodNanos;
        long refillPeriodTokens = bandwidth.refillTokens;
        long divided = multiplyExactOrReturnMaxValue(refillPeriodNanos, deficit);
        if (divided == Long.MAX_VALUE) {
            // math overflow happen.
            // there is no sense to stay in integer arithmetic when having deal with so big numbers
            return (long)((double) deficit / (double)refillPeriodTokens * (double)refillPeriodNanos);
        } else {
            long correctionForPartiallyRefilledToken = getRoundingError(bandwidthIndex);
            divided -= correctionForPartiallyRefilledToken;
            return divided / refillPeriodTokens;
        }
    }

    private long calculateDelayNanosAfterWillBePossibleToConsumeForIntervalBandwidth(int bandwidthIndex, Bandwidth bandwidth, long deficit, long currentTimeNanos) {
        long refillPeriodNanos = bandwidth.refillPeriodNanos;
        long refillTokens = bandwidth.refillTokens;
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

    long getCurrentSize(int bandwidth) {
        return stateData[bandwidth * BANDWIDTH_SIZE + 1];
    }

    private void setCurrentSize(int bandwidth, long currentSize) {
        stateData[bandwidth * BANDWIDTH_SIZE + 1] = currentSize;
    }

    private void consume(int bandwidth, long tokens) {
        stateData[bandwidth * BANDWIDTH_SIZE + 1] -= tokens;
    }

    long getRoundingError(int bandwidth) {
        return stateData[bandwidth * BANDWIDTH_SIZE + 2];
    }

    private void setRoundingError(int bandwidth, long roundingError) {
        stateData[bandwidth * BANDWIDTH_SIZE + 2] = roundingError;
    }

    public static final SerializationHandle<BucketState> SERIALIZATION_HANDLE = new SerializationHandle<BucketState>() {
        @Override
        public <S> BucketState deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            long[] data = adapter.readLongArray(input);
            return new BucketState(data);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, BucketState state) throws IOException {
            adapter.writeLongArray(output, state.stateData);
        }

        @Override
        public int getTypeId() {
            return 3;
        }

        @Override
        public Class<BucketState> getSerializedType() {
            return BucketState.class;
        }
    };

    @Override
    public String toString() {
        return "BucketState{" +
                "bandwidthStates=" + Arrays.toString(stateData) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BucketState that = (BucketState) o;
        return Arrays.equals(stateData, that.stateData);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(stateData);
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
