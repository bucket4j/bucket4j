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
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.io.NotSerializableException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class SynchronizedBucket extends AbstractBucket implements LocalBucket, ComparableByContent<SynchronizedBucket> {

    private BucketConfiguration configuration;
    private final TimeMeter timeMeter;
    private BucketState state;
    private final Lock lock;

    public SynchronizedBucket(BucketConfiguration configuration, MathType mathType, TimeMeter timeMeter, BucketListener listener) {
        this(configuration, mathType, timeMeter, listener, new ReentrantLock());
    }

    SynchronizedBucket(BucketConfiguration configuration, MathType mathType, TimeMeter timeMeter, BucketListener listener, Lock lock) {
        this(listener, timeMeter, lock, BucketState.createInitialState(configuration, mathType, timeMeter.currentTimeNanos()));
    }

    private SynchronizedBucket(BucketListener listener, TimeMeter timeMeter, Lock lock, BucketState initialState) {
        super(listener);
        this.configuration = initialState.getConfiguration();
        this.timeMeter = timeMeter;
        this.state = initialState;
        this.lock = lock;
    }

    public void setConfiguration(BucketConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Bucket toListenable(BucketListener listener) {
        return new SynchronizedBucket(listener, timeMeter, lock, state);
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = state.getAvailableTokens();
            long toConsume = Math.min(limit, availableToConsume);
            if (toConsume <= 0) {
                return 0;
            }
            state.consume(toConsume);
            return toConsume;
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = state.getAvailableTokens();
            if (tokensToConsume > availableToConsume) {
                return false;
            }
            state.consume(tokensToConsume);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected ConsumptionProbe tryConsumeAndReturnRemainingTokensImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
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
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected EstimationProbe estimateAbilityToConsumeImpl(long tokensToEstimate) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = state.getAvailableTokens();
            if (tokensToEstimate > availableToConsume) {
                long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToEstimate, currentTimeNanos, true);
                return EstimationProbe.canNotBeConsumed(availableToConsume, nanosToWaitForRefill);
            }
            return EstimationProbe.canBeConsumed(availableToConsume);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected long reserveAndCalculateTimeToSleepImpl(long tokensToConsume, long waitIfBusyNanosLimit) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            long nanosToCloseDeficit = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, false);

            if (nanosToCloseDeficit == Long.MAX_VALUE || nanosToCloseDeficit > waitIfBusyNanosLimit) {
                return Long.MAX_VALUE;
            }

            state.consume(tokensToConsume);
            return nanosToCloseDeficit;
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected long consumeIgnoringRateLimitsImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            long nanosToCloseDeficit = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, false);

            if (nanosToCloseDeficit == INFINITY_DURATION) {
                return nanosToCloseDeficit;
            }
            state.consume(tokensToConsume);
            return nanosToCloseDeficit;
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<Long> consumeAsMuchAsPossibleVerboseImpl(long limit) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = state.getAvailableTokens();
            long toConsume = Math.min(limit, availableToConsume);
            if (toConsume <= 0) {
                return new VerboseResult<>(currentTimeNanos, 0L, state.copy());
            }
            state.consume(toConsume);
            return new VerboseResult<>(currentTimeNanos, toConsume, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<Boolean> tryConsumeVerboseImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = state.getAvailableTokens();
            if (tokensToConsume > availableToConsume) {
                return new VerboseResult<>(currentTimeNanos, false, state.copy());
            }
            state.consume(tokensToConsume);
            return new VerboseResult<>(currentTimeNanos, true, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<ConsumptionProbe> tryConsumeAndReturnRemainingTokensVerboseImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
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
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<EstimationProbe> estimateAbilityToConsumeVerboseImpl(long tokensToEstimate) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = state.getAvailableTokens();
            if (tokensToEstimate > availableToConsume) {
                long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToEstimate, currentTimeNanos, true);
                EstimationProbe estimationProbe = EstimationProbe.canNotBeConsumed(availableToConsume, nanosToWaitForRefill);
                return new VerboseResult<>(currentTimeNanos, estimationProbe, state.copy());
            }
            EstimationProbe estimationProbe = EstimationProbe.canBeConsumed(availableToConsume);
            return new VerboseResult<>(currentTimeNanos, estimationProbe, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<Long> getAvailableTokensVerboseImpl() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            long availableTokens = state.getAvailableTokens();
            return new VerboseResult<>(currentTimeNanos, availableTokens, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<Nothing> addTokensVerboseImpl(long tokensToAdd) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            state.addTokens(tokensToAdd);
            return new VerboseResult<>(currentTimeNanos, Nothing.INSTANCE, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<Nothing> forceAddTokensVerboseImpl(long tokensToAdd) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            state.forceAddTokens(tokensToAdd);
            return new VerboseResult<>(currentTimeNanos, Nothing.INSTANCE, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<Nothing> resetVerboseImpl() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            state.reset();
            return new VerboseResult<>(currentTimeNanos, Nothing.INSTANCE, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<Nothing> replaceConfigurationVerboseImpl(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            this.state.refillAllBandwidth(currentTimeNanos);
            this.state = this.state.replaceConfiguration(newConfiguration, tokensInheritanceStrategy, currentTimeNanos);
            this.configuration = newConfiguration;
            return new VerboseResult<>(currentTimeNanos, null, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<Long> consumeIgnoringRateLimitsVerboseImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            long nanosToCloseDeficit = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, false);

            if (nanosToCloseDeficit == INFINITY_DURATION) {
                return new VerboseResult<>(currentTimeNanos, nanosToCloseDeficit, state.copy());
            }
            state.consume(tokensToConsume);
            return new VerboseResult<>(currentTimeNanos, nanosToCloseDeficit, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void addTokensImpl(long tokensToAdd) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            state.addTokens(tokensToAdd);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void forceAddTokensImpl(long tokensToAdd) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            state.forceAddTokens(tokensToAdd);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reset() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            state.reset();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long getAvailableTokens() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            return state.getAvailableTokens();
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void replaceConfigurationImpl(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            this.state.refillAllBandwidth(currentTimeNanos);
            this.state = this.state.replaceConfiguration(newConfiguration, tokensInheritanceStrategy, currentTimeNanos);
            this.configuration = newConfiguration;
        } finally {
            lock.unlock();
        }
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
        return SynchronizationStrategy.SYNCHRONIZED;
    }

    @Override
    public String toString() {
        synchronized (this) {
            return "SynchronizedBucket{" +
                "state=" + state +
                ", configuration=" + getConfiguration() +
                '}';
        }
    }

    public static final SerializationHandle<SynchronizedBucket> SERIALIZATION_HANDLE = new SerializationHandle<SynchronizedBucket>() {
        @Override
        public <S> SynchronizedBucket deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            BucketConfiguration bucketConfiguration = BucketConfiguration.SERIALIZATION_HANDLE.deserialize(adapter, input);
            BucketState bucketState = BucketState.deserialize(adapter, input);
            bucketState.setConfiguration(bucketConfiguration);

            return new SynchronizedBucket(BucketListener.NOPE, TimeMeter.SYSTEM_MILLISECONDS, new ReentrantLock(), bucketState);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, SynchronizedBucket bucket, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            if (bucket.timeMeter != TimeMeter.SYSTEM_MILLISECONDS) {
                throw new NotSerializableException("Only TimeMeter.SYSTEM_MILLISECONDS can be serialized safely");
            }
            adapter.writeInt(output, v_7_0_0.getNumber());
            BucketConfiguration.SERIALIZATION_HANDLE.serialize(adapter, output, bucket.state.getConfiguration(), backwardCompatibilityVersion, scope);
            BucketState.serialize(adapter, output, bucket.state, backwardCompatibilityVersion, scope);
        }

        @Override
        public int getTypeId() {
            return 61;
        }

        @Override
        public Class<SynchronizedBucket> getSerializedType() {
            return SynchronizedBucket.class;
        }

        @Override
        public SynchronizedBucket fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            Map<String, Object> stateSnapshot = (Map<String, Object>) snapshot.get("state");
            BucketState state = BucketState.fromJsonCompatibleSnapshot(stateSnapshot);

            return new SynchronizedBucket(BucketListener.NOPE, TimeMeter.SYSTEM_MILLISECONDS, new ReentrantLock(), state);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(SynchronizedBucket bucket, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            if (bucket.timeMeter != TimeMeter.SYSTEM_MILLISECONDS) {
                throw new NotSerializableException("Only TimeMeter.SYSTEM_MILLISECONDS can be serialized safely");
            }
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());
            result.put("state", BucketState.toJsonCompatibleSnapshot(bucket.state, backwardCompatibilityVersion, scope));
            return result;
        }

        @Override
        public String getTypeName() {
            return "SynchronizedBucket";
        }

    };

    @Override
    public boolean equalsByContent(SynchronizedBucket other) {
        return ComparableByContent.equals(state, other.state) &&
                ComparableByContent.equals(state.getConfiguration(), other.getConfiguration()) &&
                timeMeter == other.timeMeter;
    }

}
