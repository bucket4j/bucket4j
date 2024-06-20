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
import java.util.concurrent.atomic.AtomicReference;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;


public class LockFreeBucket extends AbstractBucket implements LocalBucket, ComparableByContent<LockFreeBucket> {

    private final AtomicReference<BucketState> stateRef;
    private final TimeMeter timeMeter;

    public LockFreeBucket(BucketConfiguration configuration, MathType mathType, TimeMeter timeMeter, BucketListener listener) {
        this(new AtomicReference<>(createStateWithConfiguration(configuration, mathType, timeMeter)), timeMeter, listener);
    }

    private LockFreeBucket(AtomicReference<BucketState> stateRef, TimeMeter timeMeter, BucketListener listener) {
        super(listener);
        this.timeMeter = timeMeter;
        this.stateRef = stateRef;
    }

    @Override
    public Bucket toListenable(BucketListener listener) {
        return new LockFreeBucket(stateRef, timeMeter, listener);
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens();
            long toConsume = Math.min(limit, availableToConsume);
            if (toConsume <= 0) {
                return 0;
            }
            newState.consume(toConsume);
            if (stateRef.compareAndSet(previousState, newState)) {
                return toConsume;
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens();
            if (tokensToConsume > availableToConsume) {
                return false;
            }
            newState.consume(tokensToConsume);
            if (stateRef.compareAndSet(previousState, newState)) {
                return true;
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected ConsumptionProbe tryConsumeAndReturnRemainingTokensImpl(long tokensToConsume) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens();
            if (tokensToConsume > availableToConsume) {
                long nanosToWaitForRefill = newState.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, true);
                long nanosToWaitForReset = newState.calculateFullRefillingTime(currentTimeNanos);
                return ConsumptionProbe.rejected(availableToConsume, nanosToWaitForRefill, nanosToWaitForReset);
            }
            newState.consume(tokensToConsume);
            if (stateRef.compareAndSet(previousState, newState)) {
                long remainingTokens = availableToConsume - tokensToConsume;
                long nanosToWaitForReset = newState.calculateFullRefillingTime(currentTimeNanos);
                return ConsumptionProbe.consumed(remainingTokens, nanosToWaitForReset);
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected EstimationProbe estimateAbilityToConsumeImpl(long tokensToEstimate) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        newState.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = newState.getAvailableTokens();
        if (tokensToEstimate > availableToConsume) {
            long nanosToWaitForRefill = newState.calculateDelayNanosAfterWillBePossibleToConsume(tokensToEstimate, currentTimeNanos, true);
            return EstimationProbe.canNotBeConsumed(availableToConsume, nanosToWaitForRefill);
        } else {
            return EstimationProbe.canBeConsumed(availableToConsume);
        }
    }

    @Override
    protected long reserveAndCalculateTimeToSleepImpl(long tokensToConsume, long waitIfBusyNanosLimit) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long nanosToCloseDeficit = newState.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, false);
            if (nanosToCloseDeficit == 0) {
                newState.consume(tokensToConsume);
                if (stateRef.compareAndSet(previousState, newState)) {
                    return 0L;
                }
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
                continue;
            }

            if (nanosToCloseDeficit == Long.MAX_VALUE || nanosToCloseDeficit > waitIfBusyNanosLimit) {
                return Long.MAX_VALUE;
            }

            newState.consume(tokensToConsume);
            if (stateRef.compareAndSet(previousState, newState)) {
                return nanosToCloseDeficit;
            }
            previousState = stateRef.get();
            newState.copyStateFrom(previousState);
        }
    }

    @Override
    protected VerboseResult<Long> reserveAndCalculateTimeToSleepVerboseImpl(long tokensToConsume, long maxWaitTimeNanos) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long nanosToCloseDeficit = newState.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, false);
            if (nanosToCloseDeficit == 0) {
                newState.consume(tokensToConsume);
                if (stateRef.compareAndSet(previousState, newState)) {
                    return new VerboseResult<>(currentTimeNanos, 0L, newState.copy());
                }
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
                continue;
            }

            if (nanosToCloseDeficit == Long.MAX_VALUE || nanosToCloseDeficit > maxWaitTimeNanos) {
                return new VerboseResult<>(currentTimeNanos, Long.MAX_VALUE, newState);
            }

            newState.consume(tokensToConsume);
            if (stateRef.compareAndSet(previousState, newState)) {
                return new VerboseResult<>(currentTimeNanos, nanosToCloseDeficit, newState.copy());
            }
            previousState = stateRef.get();
            newState.copyStateFrom(previousState);
        }
    }

    @Override
    protected void addTokensImpl(long tokensToAdd) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            newState.addTokens(tokensToAdd);
            if (stateRef.compareAndSet(previousState, newState)) {
                return;
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected void forceAddTokensImpl(long tokensToAdd) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            newState.forceAddTokens(tokensToAdd);
            if (stateRef.compareAndSet(previousState, newState)) {
                return;
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    public void reset() {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            newState.reset();
            if (stateRef.compareAndSet(previousState, newState)) {
                return;
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected void replaceConfigurationImpl(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            newState = newState.replaceConfiguration(newConfiguration, tokensInheritanceStrategy, currentTimeNanos);
            if (stateRef.compareAndSet(previousState, newState)) {
                return;
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected long consumeIgnoringRateLimitsImpl(long tokensToConsume) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long nanosToCloseDeficit = newState.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, false);

            if (nanosToCloseDeficit == INFINITY_DURATION) {
                return nanosToCloseDeficit;
            }
            newState.consume(tokensToConsume);
            if (stateRef.compareAndSet(previousState, newState)) {
                return nanosToCloseDeficit;
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected VerboseResult<Long> consumeAsMuchAsPossibleVerboseImpl(long limit) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens();
            long toConsume = Math.min(limit, availableToConsume);
            if (toConsume <= 0) {
                return new VerboseResult<>(currentTimeNanos, 0L, newState);
            }
            newState.consume(toConsume);
            if (stateRef.compareAndSet(previousState, newState)) {
                return new VerboseResult<>(currentTimeNanos, toConsume, newState.copy());
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected VerboseResult<Boolean> tryConsumeVerboseImpl(long tokensToConsume) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens();
            if (tokensToConsume > availableToConsume) {
                return new VerboseResult<>(currentTimeNanos, false, newState);
            }
            newState.consume(tokensToConsume);
            if (stateRef.compareAndSet(previousState, newState)) {
                return new VerboseResult<>(currentTimeNanos, true, newState.copy());
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected VerboseResult<ConsumptionProbe> tryConsumeAndReturnRemainingTokensVerboseImpl(long tokensToConsume) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens();
            if (tokensToConsume > availableToConsume) {
                long nanosToWaitForRefill = newState.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, true);
                long nanosToWaitForReset = newState.calculateFullRefillingTime(currentTimeNanos);
                ConsumptionProbe consumptionProbe = ConsumptionProbe.rejected(availableToConsume, nanosToWaitForRefill, nanosToWaitForReset);
                return new VerboseResult<>(currentTimeNanos, consumptionProbe, newState);
            }
            newState.consume(tokensToConsume);
            if (stateRef.compareAndSet(previousState, newState)) {
                long nanosToWaitForReset = newState.calculateFullRefillingTime(currentTimeNanos);
                ConsumptionProbe consumptionProbe = ConsumptionProbe.consumed(availableToConsume - tokensToConsume, nanosToWaitForReset);
                return new VerboseResult<>(currentTimeNanos, consumptionProbe, newState.copy());
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected VerboseResult<EstimationProbe> estimateAbilityToConsumeVerboseImpl(long tokensToEstimate) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        newState.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = newState.getAvailableTokens();
        if (tokensToEstimate > availableToConsume) {
            long nanosToWaitForRefill = newState.calculateDelayNanosAfterWillBePossibleToConsume(tokensToEstimate, currentTimeNanos, true);
            EstimationProbe estimationProbe = EstimationProbe.canNotBeConsumed(availableToConsume, nanosToWaitForRefill);
            return new VerboseResult<>(currentTimeNanos, estimationProbe, newState);
        } else {
            EstimationProbe estimationProbe = EstimationProbe.canBeConsumed(availableToConsume);
            return new VerboseResult<>(currentTimeNanos, estimationProbe, newState);
        }
    }

    @Override
    protected VerboseResult<Long> getAvailableTokensVerboseImpl() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        BucketState snapshot = stateRef.get().copy();
        snapshot.refillAllBandwidth(currentTimeNanos);
        return new VerboseResult<>(currentTimeNanos, snapshot.getAvailableTokens(), snapshot);
    }

    @Override
    protected VerboseResult<Nothing> addTokensVerboseImpl(long tokensToAdd) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            newState.addTokens(tokensToAdd);
            if (stateRef.compareAndSet(previousState, newState)) {
                return new VerboseResult<>(currentTimeNanos, Nothing.INSTANCE, newState.copy());
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected VerboseResult<Nothing> forceAddTokensVerboseImpl(long tokensToAdd) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            newState.forceAddTokens(tokensToAdd);
            if (stateRef.compareAndSet(previousState, newState)) {
                return new VerboseResult<>(currentTimeNanos, Nothing.INSTANCE, newState.copy());
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected VerboseResult<Nothing> resetVerboseImpl() {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            newState.reset();
            if (stateRef.compareAndSet(previousState, newState)) {
                return new VerboseResult<>(currentTimeNanos, Nothing.INSTANCE, newState.copy());
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected VerboseResult<Nothing> replaceConfigurationVerboseImpl(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            newState = newState.replaceConfiguration(newConfiguration, tokensInheritanceStrategy, currentTimeNanos);
            if (stateRef.compareAndSet(previousState, newState)) {
                return new VerboseResult<>(currentTimeNanos, null, newState.copy());
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected VerboseResult<Long> consumeIgnoringRateLimitsVerboseImpl(long tokensToConsume) {
        BucketState previousState = stateRef.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long nanosToCloseDeficit = newState.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, false);

            if (nanosToCloseDeficit == INFINITY_DURATION) {
                return new VerboseResult<>(currentTimeNanos, nanosToCloseDeficit, newState);
            }
            newState.consume(tokensToConsume);
            if (stateRef.compareAndSet(previousState, newState)) {
                return new VerboseResult<>(currentTimeNanos, nanosToCloseDeficit, newState.copy());
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    public long getAvailableTokens() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        BucketState snapshot = stateRef.get().copy();
        snapshot.refillAllBandwidth(currentTimeNanos);
        return snapshot.getAvailableTokens();
    }

    @Override
    public BucketConfiguration getConfiguration() {
        return stateRef.get().getConfiguration();
    }

    @Override
    public TimeMeter getTimeMeter() {
        return timeMeter;
    }

    @Override
    public ConcurrencyStrategy getSynchronizationStrategy() {
        return ConcurrencyStrategy.LOCK_FREE;
    }

    @Override
    public SerializationHandle<LocalBucket> getSerializationHandle() {
        return (SerializationHandle) SERIALIZATION_HANDLE;
    }

    private static BucketState createStateWithConfiguration(BucketConfiguration configuration, MathType mathType, TimeMeter timeMeter) {
        return BucketState.createInitialState(configuration, mathType, timeMeter.currentTimeNanos());
    }

    @Override
    public String toString() {
        BucketState bucketState = stateRef.get();
        return "LockFreeBucket{" +
                "state=" + bucketState +
                ", configuration=" + bucketState.getConfiguration() +
                '}';
    }

    public static final SerializationHandle<LockFreeBucket> SERIALIZATION_HANDLE = new SerializationHandle<>() {
        @Override
        public <S> LockFreeBucket deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            BucketConfiguration bucketConfiguration = BucketConfiguration.SERIALIZATION_HANDLE.deserialize(adapter, input);
            BucketState bucketState = BucketState.deserialize(adapter, input);
            bucketState.setConfiguration(bucketConfiguration);

            AtomicReference<BucketState> stateRef = new AtomicReference<>(bucketState);
            return new LockFreeBucket(stateRef, TimeMeter.SYSTEM_MILLISECONDS, BucketListener.NOPE);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, LockFreeBucket bucket, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            if (bucket.timeMeter != TimeMeter.SYSTEM_MILLISECONDS) {
                throw new NotSerializableException("Only TimeMeter.SYSTEM_MILLISECONDS can be serialized safely");
            }
            adapter.writeInt(output, v_7_0_0.getNumber());
            BucketState state = bucket.stateRef.get();
            BucketConfiguration.SERIALIZATION_HANDLE.serialize(adapter, output, state.getConfiguration(), backwardCompatibilityVersion, scope);
            BucketState.serialize(adapter, output, state, backwardCompatibilityVersion, scope);
        }

        @Override
        public int getTypeId() {
            return 60;
        }

        @Override
        public Class<LockFreeBucket> getSerializedType() {
            return LockFreeBucket.class;
        }

        @Override
        public LockFreeBucket fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            Map<String, Object> stateSnapshot = (Map<String, Object>) snapshot.get("state");
            BucketState state = BucketState.fromJsonCompatibleSnapshot(stateSnapshot);

            return new LockFreeBucket(new AtomicReference<>(state), TimeMeter.SYSTEM_MILLISECONDS, BucketListener.NOPE);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(LockFreeBucket bucket, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            if (bucket.timeMeter != TimeMeter.SYSTEM_MILLISECONDS) {
                throw new NotSerializableException("Only TimeMeter.SYSTEM_MILLISECONDS can be serialized safely");
            }
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());
            result.put("state", BucketState.toJsonCompatibleSnapshot(bucket.stateRef.get(), backwardCompatibilityVersion, scope));
            return result;
        }

        @Override
        public String getTypeName() {
            return "LockFreeBucket";
        }

    };

    @Override
    public boolean equalsByContent(LockFreeBucket other) {
        BucketState state = stateRef.get();
        BucketState otherState = other.stateRef.get();
        return ComparableByContent.equals(state, otherState) &&
                ComparableByContent.equals(state.getConfiguration(), otherState.getConfiguration()) &&
                timeMeter == other.timeMeter;
    }

}
