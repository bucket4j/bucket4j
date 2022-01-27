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

import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.Arrays;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class BucketStateIEEE754 implements BucketState, ComparableByContent<BucketStateIEEE754> {

    // holds the current amount of tokens per each bandwidth
    double[] tokens;

    // holds the last refill time per each bandwidth
    long[] lastRefillTime;

    BucketConfiguration configuration;

    public static SerializationHandle<BucketStateIEEE754> SERIALIZATION_HANDLE = new SerializationHandle<BucketStateIEEE754>() {

        @Override
        public <S> BucketStateIEEE754 deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            double[] tokens = adapter.readDoubleArray(input);
            long[] lastRefillTime = adapter.readLongArray(input);
            return new BucketStateIEEE754(tokens, lastRefillTime);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, BucketStateIEEE754 state, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            adapter.writeDoubleArray(output, state.tokens);
            adapter.writeLongArray(output, state.lastRefillTime);
        }

        @Override
        public int getTypeId() {
            return 4;
        }

        @Override
        public Class<BucketStateIEEE754> getSerializedType() {
            return BucketStateIEEE754.class;
        }

    };

    BucketStateIEEE754(double[] tokens, long[] lastRefillTime) {
        this.tokens = tokens;
        this.lastRefillTime = lastRefillTime;
    }

    public BucketStateIEEE754(BucketConfiguration configuration, long currentTimeNanos) {
        this.configuration = configuration;
        Bandwidth[] bandwidths = configuration.getBandwidths();

        this.tokens = new double[bandwidths.length];
        this.lastRefillTime = new long[bandwidths.length];
        for(int i = 0; i < bandwidths.length; i++) {
            tokens[i] = calculateInitialTokens(bandwidths[i], currentTimeNanos);
            lastRefillTime[i] = calculateLastRefillTimeNanos(bandwidths[i], currentTimeNanos);
        }
    }

    @Override
    public BucketConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(BucketConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public BucketState replaceConfiguration(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy, long currentTimeNanos) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public void forceAddTokens(long tokensToAdd) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public BucketStateIEEE754 copy() {
        BucketStateIEEE754 copy = new BucketStateIEEE754(tokens.clone(), lastRefillTime.clone());
        copy.setConfiguration(configuration);
        return copy;
    }

    @Override
    public void copyStateFrom(BucketState sourceState) {
        BucketStateIEEE754 sourceStateIEEE754 = (BucketStateIEEE754) sourceState;
        if (sourceStateIEEE754.configuration == configuration) {
            System.arraycopy(sourceStateIEEE754.tokens, 0, tokens, 0, tokens.length);
            System.arraycopy(sourceStateIEEE754.lastRefillTime, 0, lastRefillTime, 0, lastRefillTime.length);
        } else {
            this.configuration = sourceStateIEEE754.configuration;
            this.lastRefillTime = sourceStateIEEE754.lastRefillTime.clone();
            this.tokens = sourceStateIEEE754.tokens.clone();
        }
    }

    @Override
    public long getCurrentSize(int bandwidth) {
        return (long) tokens[bandwidth];
    }

    @Override
    public long getRoundingError(int bandwidth) {
        // accumulated computational error is always zero for this type of state
        return 0;
    }

    @Override
    public MathType getMathType() {
        return MathType.IEEE_754;
    }

    @Override
    public long getAvailableTokens() {
        long availableTokens = (long) tokens[0];
        for (int i = 1; i < tokens.length; i++) {
            availableTokens = Math.min(availableTokens, (long) tokens[i]);
        }
        return availableTokens;
    }

    @Override
    public void consume(long toConsume) {
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] -= toConsume;
        }
    }

    @Override
    public void addTokens(long tokensToAdd) {
        Bandwidth[] bandwidths = configuration.getBandwidths();
        for (int i = 0; i < bandwidths.length; i++) {
            double currentSize = tokens[i];
            double newSize = currentSize + tokensToAdd;
            if (newSize >= bandwidths[i].getCapacity()) {
                tokens[i] = bandwidths[i].getCapacity();
            } else {
                tokens[i] = newSize;
            }
        }
    }

    @Override
    public void refillAllBandwidth(long currentTimeNanos) {
        Bandwidth[] limits = configuration.getBandwidths();
        for (int i = 0; i < limits.length; i++) {
            refill(i, limits[i], currentTimeNanos);
        }
    }

    private void refill(int bandwidthIndex, Bandwidth bandwidth, long currentTimeNanos) {
        long previousRefillNanos = lastRefillTime[bandwidthIndex];
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
            lastRefillTime[bandwidthIndex] = currentTimeNanos;
        }

        final long capacity = bandwidth.getCapacity();
        final long refillPeriodNanos = bandwidth.getRefillPeriodNanos();
        final long refillTokens = bandwidth.getRefillTokens();
        double newSize = tokens[bandwidthIndex];

        long durationSinceLastRefillNanos = currentTimeNanos - previousRefillNanos;
        if (durationSinceLastRefillNanos > refillPeriodNanos) {
            long elapsedPeriods = durationSinceLastRefillNanos / refillPeriodNanos;
            long calculatedRefill = elapsedPeriods * refillTokens;
            newSize += calculatedRefill;
            if (newSize > capacity) {
                tokens[bandwidthIndex] = capacity;
                return;
            }

            durationSinceLastRefillNanos %= refillPeriodNanos;
        }

        double calculatedRefill = (double) durationSinceLastRefillNanos / (double) refillPeriodNanos * (double) refillTokens;
        newSize += calculatedRefill;
        if (newSize >= capacity) {
            newSize = capacity;
        }
        tokens[bandwidthIndex] = newSize;
    }

    @Override
    public long calculateDelayNanosAfterWillBePossibleToConsume(long tokensToConsume, long currentTimeNanos) {
        Bandwidth[] bandwidths = configuration.getBandwidths();
        long delayAfterWillBePossibleToConsume = calculateDelayNanosAfterWillBePossibleToConsumeForBandwidth(0, bandwidths[0], tokensToConsume, currentTimeNanos);
        for (int i = 1; i < bandwidths.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            long delay = calculateDelayNanosAfterWillBePossibleToConsumeForBandwidth(i, bandwidth, tokensToConsume, currentTimeNanos);
            delayAfterWillBePossibleToConsume = Math.max(delayAfterWillBePossibleToConsume, delay);
        }
        return delayAfterWillBePossibleToConsume;
    }

    @Override
    public long calculateFullRefillingTime(long currentTimeNanos) {
        Bandwidth[] bandwidths = configuration.getBandwidths();
        long maxTimeToFullRefillNanos = calculateFullRefillingTime(0, bandwidths[0], currentTimeNanos);
        for (int i = 1; i < bandwidths.length; i++) {
            maxTimeToFullRefillNanos = Math.max(maxTimeToFullRefillNanos, calculateFullRefillingTime(i, bandwidths[i], currentTimeNanos));
        }
        return maxTimeToFullRefillNanos;
    }

    private long calculateFullRefillingTime(int bandwidthIndex, Bandwidth bandwidth, long currentTimeNanos) {
        long availableTokens = getCurrentSize(bandwidthIndex);
        if (availableTokens >= bandwidth.capacity) {
            return 0L;
        }
        double deficit = bandwidth.capacity - availableTokens;

        double nanosToWait;
        if (bandwidth.isRefillIntervally()) {
            nanosToWait = calculateDelayNanosAfterWillBePossibleToConsumeForIntervalBandwidth(bandwidthIndex, bandwidth, deficit, currentTimeNanos);
        } else {
            nanosToWait = calculateDelayNanosAfterWillBePossibleToConsumeForGreedyBandwidth(bandwidth, deficit);
        }
        return nanosToWait < Long.MAX_VALUE? (long) nanosToWait : Long.MAX_VALUE;
    }

    private long calculateDelayNanosAfterWillBePossibleToConsumeForBandwidth(int bandwidthIndex, Bandwidth bandwidth, long tokensToConsume, long currentTimeNanos) {
        double currentSize = tokens[bandwidthIndex];
        if (tokensToConsume <= currentSize) {
            return 0;
        }
        double deficit = tokensToConsume - currentSize;

        double nanosToWait;
        if (bandwidth.isRefillIntervally()) {
            nanosToWait = calculateDelayNanosAfterWillBePossibleToConsumeForIntervalBandwidth(bandwidthIndex, bandwidth, deficit, currentTimeNanos);
        } else {
            nanosToWait = calculateDelayNanosAfterWillBePossibleToConsumeForGreedyBandwidth(bandwidth, deficit);
        }
        return nanosToWait < Long.MAX_VALUE? (long) nanosToWait : Long.MAX_VALUE;
    }

    private double calculateDelayNanosAfterWillBePossibleToConsumeForGreedyBandwidth(Bandwidth bandwidth, double deficit) {
        return (double) bandwidth.getRefillPeriodNanos() * deficit /(double) bandwidth.getRefillTokens();
    }

    private double calculateDelayNanosAfterWillBePossibleToConsumeForIntervalBandwidth(int bandwidthIndex, Bandwidth bandwidth, double deficit, long currentTimeNanos) {
        long refillPeriodNanos = bandwidth.getRefillPeriodNanos();
        long refillTokens = bandwidth.getRefillTokens();
        long previousRefillNanos = lastRefillTime[bandwidthIndex];

        long timeOfNextRefillNanos = previousRefillNanos + refillPeriodNanos;
        long waitForNextRefillNanos = timeOfNextRefillNanos - currentTimeNanos;
        if (deficit <= refillTokens) {
            return waitForNextRefillNanos;
        }

        deficit -= refillTokens;
        double deficitPeriodsAsDouble = Math.ceil(deficit / refillTokens);

        double deficitNanos = deficitPeriodsAsDouble * refillPeriodNanos;
        return deficitNanos + waitForNextRefillNanos;
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

        return Math.min(
                bandwidth.capacity,
                guaranteedBase + (long)((double)nanosBeforeFirstRefill * (double) bandwidth.refillTokens / (double) bandwidth.refillPeriodNanos));
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BucketState{");
        sb.append("tokens=").append(Arrays.toString(tokens));
        sb.append(", lastRefillTime=").append(Arrays.toString(lastRefillTime));
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equalsByContent(BucketStateIEEE754 other) {
        return Arrays.equals(tokens, other.tokens) &&
                Arrays.equals(lastRefillTime, other.lastRefillTime);
    }

}
