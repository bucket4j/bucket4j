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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class LockFreeBucket extends AbstractBucket implements LocalBucket {

    private final TimeMeter timeMeter;

    private final AtomicReference<StateWithConfiguration> stateRef;

    public LockFreeBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
        this(new AtomicReference<>(createStateWithConfiguration(configuration, timeMeter)), timeMeter, BucketListener.NOPE);
    }

    private LockFreeBucket(AtomicReference<StateWithConfiguration> stateRef, TimeMeter timeMeter, BucketListener listener) {
        super(listener);
        this.timeMeter = timeMeter;
        this.stateRef = stateRef;
    }

    @Override
    public Bucket toListenable(BucketListener listener) {
        return new LockFreeBucket(stateRef, timeMeter, listener);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens();
            long toConsume = Math.min(limit, availableToConsume);
            if (toConsume == 0) {
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
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
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
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens();
            if (tokensToConsume > availableToConsume) {
                long nanosToWaitForRefill = newState.delayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);
                return ConsumptionProbe.rejected(availableToConsume, nanosToWaitForRefill);
            }
            newState.consume(tokensToConsume);
            if (stateRef.compareAndSet(previousState, newState)) {
                return ConsumptionProbe.consumed(availableToConsume - tokensToConsume);
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected EstimationProbe estimateAbilityToConsumeImpl(long tokensToEstimate) {
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        newState.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = newState.getAvailableTokens();
        if (tokensToEstimate > availableToConsume) {
            long nanosToWaitForRefill = newState.delayNanosAfterWillBePossibleToConsume(tokensToEstimate, currentTimeNanos);
            return EstimationProbe.canNotBeConsumed(availableToConsume, nanosToWaitForRefill);
        } else {
            return EstimationProbe.canBeConsumed(availableToConsume);
        }
    }

    @Override
    protected long reserveAndCalculateTimeToSleepImpl(long tokensToConsume, long waitIfBusyNanosLimit) {
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long nanosToCloseDeficit = newState.delayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);
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
    protected void addTokensImpl(long tokensToAdd) {
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            newState.state.addTokens(newState.configuration.getBandwidths(), tokensToAdd);
            if (stateRef.compareAndSet(previousState, newState)) {
                return;
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected BucketConfiguration replaceConfigurationImpl(BucketConfiguration newConfiguration) {
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            if (!previousState.configuration.isCompatible(newConfiguration)) {
                return previousState.configuration;
            }
            newState.refillAllBandwidth(currentTimeNanos);
            newState.configuration = newConfiguration;
            if (stateRef.compareAndSet(previousState, newState)) {
                return null;
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected long consumeIgnoringRateLimitsImpl(long tokensToConsume) {
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long nanosToCloseDeficit = newState.delayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);

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
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens();
            long toConsume = Math.min(limit, availableToConsume);
            if (toConsume == 0) {
                return new VerboseResult<>(currentTimeNanos, 0L, newState.configuration, newState.state);
            }
            newState.consume(toConsume);
            if (stateRef.compareAndSet(previousState, newState)) {
                return new VerboseResult<>(currentTimeNanos, toConsume, newState.configuration, newState.state.copy());
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected VerboseResult<Boolean> tryConsumeVerboseImpl(long tokensToConsume) {
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens();
            if (tokensToConsume > availableToConsume) {
                return new VerboseResult<>(currentTimeNanos, false, newState.configuration, newState.state);
            }
            newState.consume(tokensToConsume);
            if (stateRef.compareAndSet(previousState, newState)) {
                return new VerboseResult<>(currentTimeNanos, true, newState.configuration, newState.state.copy());
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected VerboseResult<ConsumptionProbe> tryConsumeAndReturnRemainingTokensVerboseImpl(long tokensToConsume) {
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens();
            if (tokensToConsume > availableToConsume) {
                long nanosToWaitForRefill = newState.delayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);
                ConsumptionProbe consumptionProbe = ConsumptionProbe.rejected(availableToConsume, nanosToWaitForRefill);
                return new VerboseResult<>(currentTimeNanos, consumptionProbe, newState.configuration, newState.state);
            }
            newState.consume(tokensToConsume);
            if (stateRef.compareAndSet(previousState, newState)) {
                ConsumptionProbe consumptionProbe = ConsumptionProbe.consumed(availableToConsume - tokensToConsume);
                return new VerboseResult<>(currentTimeNanos, consumptionProbe, newState.configuration, newState.state.copy());
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected VerboseResult<EstimationProbe> estimateAbilityToConsumeVerboseImpl(long tokensToEstimate) {
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        newState.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = newState.getAvailableTokens();
        if (tokensToEstimate > availableToConsume) {
            long nanosToWaitForRefill = newState.delayNanosAfterWillBePossibleToConsume(tokensToEstimate, currentTimeNanos);
            EstimationProbe estimationProbe = EstimationProbe.canNotBeConsumed(availableToConsume, nanosToWaitForRefill);
            return new VerboseResult<>(currentTimeNanos, estimationProbe, newState.configuration, newState.state);
        } else {
            EstimationProbe estimationProbe = EstimationProbe.canBeConsumed(availableToConsume);
            return new VerboseResult<>(currentTimeNanos, estimationProbe, newState.configuration, newState.state);
        }
    }

    @Override
    protected VerboseResult<Long> getAvailableTokensVerboseImpl() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        StateWithConfiguration snapshot = stateRef.get().copy();
        snapshot.refillAllBandwidth(currentTimeNanos);
        return new VerboseResult<>(currentTimeNanos, snapshot.getAvailableTokens(), snapshot.configuration, snapshot.state);
    }

    @Override
    protected VerboseResult<Nothing> addTokensVerboseImpl(long tokensToAdd) {
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            newState.state.addTokens(newState.configuration.getBandwidths(), tokensToAdd);
            if (stateRef.compareAndSet(previousState, newState)) {
                return new VerboseResult<>(currentTimeNanos, Nothing.INSTANCE, newState.configuration, newState.state.copy());
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected VerboseResult<BucketConfiguration> replaceConfigurationVerboseImpl(BucketConfiguration newConfiguration) {
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            if (!previousState.configuration.isCompatible(newConfiguration)) {
                return new VerboseResult<>(currentTimeNanos, null, previousState.configuration, newState.state.copy());
            }
            newState.refillAllBandwidth(currentTimeNanos);
            newState.configuration = newConfiguration;
            if (stateRef.compareAndSet(previousState, newState)) {
                return new VerboseResult<>(currentTimeNanos, null, newState.configuration, newState.state.copy());
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected VerboseResult<Long> consumeIgnoringRateLimitsVerboseImpl(long tokensToConsume) {
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long nanosToCloseDeficit = newState.delayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);

            if (nanosToCloseDeficit == INFINITY_DURATION) {
                return new VerboseResult<>(currentTimeNanos, nanosToCloseDeficit, newState.configuration, newState.state);
            }
            newState.consume(tokensToConsume);
            if (stateRef.compareAndSet(previousState, newState)) {
                return new VerboseResult<>(currentTimeNanos, nanosToCloseDeficit, newState.configuration, newState.state.copy());
            } else {
                previousState = stateRef.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    public long getAvailableTokens() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        StateWithConfiguration snapshot = stateRef.get().copy();
        snapshot.refillAllBandwidth(currentTimeNanos);
        return snapshot.getAvailableTokens();
    }

    @Override
    protected CompletableFuture<Boolean> tryConsumeAsyncImpl(long tokensToConsume) {
        boolean result = tryConsumeImpl(tokensToConsume);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    protected CompletableFuture<Void> addTokensAsyncImpl(long tokensToAdd) {
        addTokensImpl(tokensToAdd);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<BucketConfiguration> replaceConfigurationAsyncImpl(BucketConfiguration newConfiguration) {
        return CompletableFuture.completedFuture(replaceConfigurationImpl(newConfiguration));
    }

    @Override
    protected CompletableFuture<Long> consumeIgnoringRateLimitsAsyncImpl(long tokensToConsume) {
        return CompletableFuture.completedFuture(consumeIgnoringRateLimitsImpl(tokensToConsume));
    }

    @Override
    protected CompletableFuture<ConsumptionProbe> tryConsumeAndReturnRemainingTokensAsyncImpl(long tokensToConsume) {
        ConsumptionProbe result = tryConsumeAndReturnRemainingTokensImpl(tokensToConsume);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    protected CompletableFuture<EstimationProbe> estimateAbilityToConsumeAsyncImpl(long tokensToEstimate) {
        EstimationProbe result = estimateAbilityToConsumeImpl(tokensToEstimate);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    protected CompletableFuture<Long> tryConsumeAsMuchAsPossibleAsyncImpl(long limit) {
        long result = consumeAsMuchAsPossibleImpl(limit);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    protected CompletableFuture<Long> reserveAndCalculateTimeToSleepAsyncImpl(long tokensToConsume, long maxWaitTimeNanos) {
        long result = reserveAndCalculateTimeToSleepImpl(tokensToConsume, maxWaitTimeNanos);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    protected CompletableFuture<VerboseResult<Long>> tryConsumeAsMuchAsPossibleVerboseAsyncImpl(long limit) {
        VerboseResult<Long> result = consumeAsMuchAsPossibleVerboseImpl(limit);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    protected CompletableFuture<VerboseResult<Boolean>> tryConsumeVerboseAsyncImpl(long tokensToConsume) {
        VerboseResult<Boolean> result = tryConsumeVerboseImpl(tokensToConsume);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    protected CompletableFuture<VerboseResult<ConsumptionProbe>> tryConsumeAndReturnRemainingTokensVerboseAsyncImpl(long tokensToConsume) {
        VerboseResult<ConsumptionProbe> result = tryConsumeAndReturnRemainingTokensVerboseImpl(tokensToConsume);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    protected CompletableFuture<VerboseResult<EstimationProbe>> estimateAbilityToConsumeVerboseAsyncImpl(long tokensToEstimate) {
        VerboseResult<EstimationProbe> result = estimateAbilityToConsumeVerboseImpl(tokensToEstimate);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    protected CompletableFuture<VerboseResult<Nothing>> addTokensVerboseAsyncImpl(long tokensToAdd) {
        VerboseResult<Nothing> result = addTokensVerboseImpl(tokensToAdd);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    protected CompletableFuture<VerboseResult<BucketConfiguration>> replaceConfigurationVerboseAsyncImpl(BucketConfiguration newConfiguration) {
        VerboseResult<BucketConfiguration> result = replaceConfigurationVerboseImpl(newConfiguration);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    protected CompletableFuture<VerboseResult<Long>> consumeIgnoringRateLimitsVerboseAsyncImpl(long tokensToConsume) {
        VerboseResult<Long> result = consumeIgnoringRateLimitsVerboseImpl(tokensToConsume);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public BucketState createSnapshot() {
        return stateRef.get().state.copy();
    }

    @Override
    public BucketConfiguration getConfiguration() {
        return stateRef.get().configuration;
    }

    private static class StateWithConfiguration {

        BucketConfiguration configuration;
        BucketState state;

        StateWithConfiguration(BucketConfiguration configuration, BucketState state) {
            this.configuration = configuration;
            this.state = state;
        }

        StateWithConfiguration copy() {
            return new StateWithConfiguration(configuration, state.copy());
        }

        void copyStateFrom(StateWithConfiguration other) {
            configuration = other.configuration;
            state.copyStateFrom(other.state);
        }

        void refillAllBandwidth(long currentTimeNanos) {
            state.refillAllBandwidth(configuration.getBandwidths(), currentTimeNanos);
        }

        long getAvailableTokens() {
            return state.getAvailableTokens(configuration.getBandwidths());
        }

        void consume(long tokensToConsume) {
            state.consume(configuration.getBandwidths(), tokensToConsume);
        }

        long delayNanosAfterWillBePossibleToConsume(long tokensToConsume, long currentTimeNanos) {
            return state.calculateDelayNanosAfterWillBePossibleToConsume(configuration.getBandwidths(), tokensToConsume, currentTimeNanos);
        }
    }

    private static StateWithConfiguration createStateWithConfiguration(BucketConfiguration configuration, TimeMeter timeMeter) {
        BucketState initialState = BucketState.createInitialState(configuration, timeMeter.currentTimeNanos());
        return new StateWithConfiguration(configuration, initialState);
    }

    @Override
    public String toString() {
        return "LockFreeBucket{" +
                "state=" + stateRef.get() +
                ", configuration=" + getConfiguration() +
                '}';
    }

}
