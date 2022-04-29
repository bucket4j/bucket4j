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

package io.github.bucket4j.local;


import io.github.bucket4j.*;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.io.NotSerializableException;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class ThreadUnsafeBucket extends AbstractBucket implements LocalBucket, ComparableByContent<ThreadUnsafeBucket> {

    private BucketConfiguration configuration;
    private final TimeMeter timeMeter;
    private BucketState state;

    public ThreadUnsafeBucket(BucketConfiguration configuration, MathType mathType, TimeMeter timeMeter) {
        this(BucketListener.NOPE, timeMeter, BucketState.createInitialState(configuration, mathType, timeMeter.currentTimeNanos()));
    }

    private ThreadUnsafeBucket(BucketListener listener, TimeMeter timeMeter, BucketState initialState) {
        super(listener);
        this.configuration = initialState.getConfiguration();
        this.timeMeter = timeMeter;
        this.state = initialState;
    }

    public void setConfiguration(BucketConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Bucket toListenable(BucketListener listener) {
        return new ThreadUnsafeBucket(listener, timeMeter, state);
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        long toConsume = Math.min(limit, availableToConsume);
        if (toConsume == 0) {
            return 0;
        }
        state.consume(toConsume);
        return toConsume;
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToConsume > availableToConsume) {
            return false;
        }
        state.consume(tokensToConsume);
        return true;
    }

    @Override
    protected ConsumptionProbe tryConsumeAndReturnRemainingTokensImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToConsume > availableToConsume) {
            long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, true);
            long nanosToWaitForReset = state.calculateFullRefillingTime(currentTimeNanos);
            return ConsumptionProbe.rejected(availableToConsume, nanosToWaitForRefill, nanosToWaitForReset);
        }
        state.consume(tokensToConsume);
        long remainingTokens = availableToConsume - tokensToConsume;
        long nanosToWaitForReset = state.calculateFullRefillingTime(currentTimeNanos);
        return ConsumptionProbe.consumed(remainingTokens, nanosToWaitForReset);
    }

    @Override
    protected EstimationProbe estimateAbilityToConsumeImpl(long tokensToEstimate) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToEstimate > availableToConsume) {
            long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToEstimate, currentTimeNanos, true);
            return EstimationProbe.canNotBeConsumed(availableToConsume, nanosToWaitForRefill);
        }
        return EstimationProbe.canBeConsumed(availableToConsume);
    }

    @Override
    protected long reserveAndCalculateTimeToSleepImpl(long tokensToConsume, long waitIfBusyNanosLimit) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        long nanosToCloseDeficit = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, false);

        if (nanosToCloseDeficit == Long.MAX_VALUE || nanosToCloseDeficit > waitIfBusyNanosLimit) {
            return Long.MAX_VALUE;
        }

        state.consume(tokensToConsume);
        return nanosToCloseDeficit;
    }

    @Override
    protected long consumeIgnoringRateLimitsImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        long nanosToCloseDeficit = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, false);

        if (nanosToCloseDeficit == INFINITY_DURATION) {
            return nanosToCloseDeficit;
        }
        state.consume(tokensToConsume);
        return nanosToCloseDeficit;
    }

    @Override
    protected VerboseResult<Long> consumeAsMuchAsPossibleVerboseImpl(long limit) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        long toConsume = Math.min(limit, availableToConsume);
        if (toConsume == 0) {
            return new VerboseResult<>(currentTimeNanos, 0L, state.copy());
        }
        state.consume(toConsume);
        return new VerboseResult<>(currentTimeNanos, toConsume, state.copy());
    }

    @Override
    protected VerboseResult<Boolean> tryConsumeVerboseImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToConsume > availableToConsume) {
            return new VerboseResult<>(currentTimeNanos, false, state.copy());
        }
        state.consume(tokensToConsume);
        return new VerboseResult<>(currentTimeNanos, true, state.copy());
    }

    @Override
    protected VerboseResult<ConsumptionProbe> tryConsumeAndReturnRemainingTokensVerboseImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToConsume > availableToConsume) {
            long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, true);
            long nanosToWaitForReset = state.calculateFullRefillingTime(currentTimeNanos);
            ConsumptionProbe probe = ConsumptionProbe.rejected(availableToConsume, nanosToWaitForRefill, nanosToWaitForReset);
            return new VerboseResult<>(currentTimeNanos, probe, state.copy());
        }
        state.consume(tokensToConsume);
        long nanosToWaitForReset = state.calculateFullRefillingTime(currentTimeNanos);
        ConsumptionProbe probe = ConsumptionProbe.consumed(availableToConsume - tokensToConsume, nanosToWaitForReset);
        return new VerboseResult<>(currentTimeNanos, probe, state.copy());
    }

    @Override
    protected VerboseResult<EstimationProbe> estimateAbilityToConsumeVerboseImpl(long tokensToEstimate) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToEstimate > availableToConsume) {
            long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToEstimate, currentTimeNanos, true);
            EstimationProbe estimationProbe = EstimationProbe.canNotBeConsumed(availableToConsume, nanosToWaitForRefill);
            return new VerboseResult<>(currentTimeNanos, estimationProbe, state.copy());
        }
        EstimationProbe estimationProbe = EstimationProbe.canBeConsumed(availableToConsume);
        return new VerboseResult<>(currentTimeNanos, estimationProbe, state.copy());
    }

    @Override
    protected VerboseResult<Long> getAvailableTokensVerboseImpl() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        long availableTokens = state.getAvailableTokens();
        return new VerboseResult<>(currentTimeNanos, availableTokens, state.copy());
    }

    @Override
    protected VerboseResult<Nothing> addTokensVerboseImpl(long tokensToAdd) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        state.addTokens(tokensToAdd);
        return new VerboseResult<>(currentTimeNanos, Nothing.INSTANCE, state.copy());
    }

    @Override
    protected VerboseResult<Nothing> forceAddTokensVerboseImpl(long tokensToAdd) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        state.forceAddTokens(tokensToAdd);
        return new VerboseResult<>(currentTimeNanos, Nothing.INSTANCE, state.copy());
    }

    @Override
    protected VerboseResult<Nothing> resetVerboseImpl() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        state.reset();
        return new VerboseResult<>(currentTimeNanos, Nothing.INSTANCE, state.copy());
    }

    @Override
    protected VerboseResult<Nothing> replaceConfigurationVerboseImpl(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        this.state.refillAllBandwidth(currentTimeNanos);
        this.state = this.state.replaceConfiguration(newConfiguration, tokensInheritanceStrategy, currentTimeNanos);
        this.configuration = newConfiguration;
        return new VerboseResult<>(currentTimeNanos, null, state.copy());
    }

    @Override
    protected VerboseResult<Long> consumeIgnoringRateLimitsVerboseImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        long nanosToCloseDeficit = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, false);

        if (nanosToCloseDeficit == INFINITY_DURATION) {
            return new VerboseResult<>(currentTimeNanos, nanosToCloseDeficit, state.copy());
        }
        state.consume(tokensToConsume);
        return new VerboseResult<>(currentTimeNanos, nanosToCloseDeficit, state.copy());
    }

    @Override
    protected void addTokensImpl(long tokensToAdd) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        state.addTokens(tokensToAdd);
    }

    @Override
    protected void forceAddTokensImpl(long tokensToAdd) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        state.forceAddTokens(tokensToAdd);
    }

    @Override
    public void reset() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        state.reset();
    }

    @Override
    public long getAvailableTokens() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);
        return state.getAvailableTokens();
    }

    @Override
    protected void replaceConfigurationImpl(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        this.state.refillAllBandwidth(currentTimeNanos);
        this.state = this.state.replaceConfiguration(newConfiguration, tokensInheritanceStrategy, currentTimeNanos);
        this.configuration = newConfiguration;
    }

    @Override
    public BucketConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public TimeMeter getTimeMeter() {
        return timeMeter;
    }

    @Override
    public SynchronizationStrategy getSynchronizationStrategy() {
        return SynchronizationStrategy.NONE;
    }

    @Override
    public String toString() {
        synchronized (this) {
            return "ThreadUnsafeBucket{" +
                "state=" + state +
                ", configuration=" + getConfiguration() +
                '}';
        }
    }

    public static final SerializationHandle<ThreadUnsafeBucket> SERIALIZATION_HANDLE = new SerializationHandle<ThreadUnsafeBucket>() {
        @Override
        public <S> ThreadUnsafeBucket deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            BucketConfiguration bucketConfiguration = BucketConfiguration.SERIALIZATION_HANDLE.deserialize(adapter, input, backwardCompatibilityVersion);
            BucketState bucketState = BucketState.deserialize(adapter, input, backwardCompatibilityVersion);
            bucketState.setConfiguration(bucketConfiguration);

            return new ThreadUnsafeBucket(BucketListener.NOPE, TimeMeter.SYSTEM_MILLISECONDS, bucketState);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ThreadUnsafeBucket bucket, Version backwardCompatibilityVersion) throws IOException {
            if (bucket.timeMeter != TimeMeter.SYSTEM_MILLISECONDS) {
                throw new NotSerializableException("Only TimeMeter.SYSTEM_MILLISECONDS can be serialized safely");
            }
            adapter.writeInt(output, v_7_0_0.getNumber());
            BucketConfiguration.SERIALIZATION_HANDLE.serialize(adapter, output, bucket.state.getConfiguration(), backwardCompatibilityVersion);
            BucketState.serialize(adapter, output, bucket.state, backwardCompatibilityVersion);
        }

        @Override
        public int getTypeId() {
            return 62;
        }

        @Override
        public Class<ThreadUnsafeBucket> getSerializedType() {
            return ThreadUnsafeBucket.class;
        }

    };

    @Override
    public boolean equalsByContent(ThreadUnsafeBucket other) {
        return ComparableByContent.equals(state, other.state) &&
                ComparableByContent.equals(state.getConfiguration(), other.getConfiguration()) &&
                timeMeter == other.timeMeter;
    }

}
