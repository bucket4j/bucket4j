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

package io.github.bucket4j.local;

import io.github.bucket4j.*;

import java.util.concurrent.atomic.AtomicReference;

public class LockFreeBucket extends AbstractBucket implements LocalBucket {

    private final TimeMeter timeMeter;

    private final AtomicReference<StateWithConfiguration> stateRef;

    public LockFreeBucket(BucketConfiguration configuration, MathType mathType, TimeMeter timeMeter) {
        this(new AtomicReference<>(createStateWithConfiguration(configuration, mathType, timeMeter)), timeMeter, BucketListener.NOPE);
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
    protected void replaceConfigurationImpl(BucketConfiguration newConfiguration) {
        StateWithConfiguration previousState = stateRef.get();
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            previousState.configuration.checkCompatibility(newConfiguration);
            newState.refillAllBandwidth(currentTimeNanos);
            newState.configuration = newConfiguration;
            if (stateRef.compareAndSet(previousState, newState)) {
                return;
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
    public BucketState createSnapshot() {
        return stateRef.get().state.copy();
    }

    @Override
    public BucketConfiguration getConfiguration() {
        return stateRef.get().configuration;
    }

    @Override
    public TimeMeter getTimeMeter() {
        return timeMeter;
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

        long calculateFullRefillingTime(long currentTimeNanos) {
            return state.calculateFullRefillingTime(configuration.getBandwidths(), currentTimeNanos);
        }

        long delayNanosAfterWillBePossibleToConsume(long tokensToConsume, long currentTimeNanos) {
            return state.calculateDelayNanosAfterWillBePossibleToConsume(configuration.getBandwidths(), tokensToConsume, currentTimeNanos);
        }
    }

    private static StateWithConfiguration createStateWithConfiguration(BucketConfiguration configuration, MathType mathType, TimeMeter timeMeter) {
        BucketState initialState = BucketState.createInitialState(configuration, mathType, timeMeter.currentTimeNanos());
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