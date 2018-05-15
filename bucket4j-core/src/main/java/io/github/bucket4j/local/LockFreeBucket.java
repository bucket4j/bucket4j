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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class LockFreeBucket extends AbstractBucket implements LocalBucket {

    private final TimeMeter timeMeter;
    private static final AtomicReferenceFieldUpdater<LockFreeBucket, StateWithConfiguration> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(LockFreeBucket.class, StateWithConfiguration.class, "state");
    private volatile StateWithConfiguration state;

    public LockFreeBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
        this(createStateWithConfiguration(configuration, timeMeter), timeMeter, BucketListener.NOPE);
    }

    private LockFreeBucket(StateWithConfiguration state, TimeMeter timeMeter, BucketListener listener) {
        super(listener);
        this.timeMeter = timeMeter;
        this.state = state;
    }

    @Override
    public Bucket toListenable(BucketListener listener) {
        return new LockFreeBucket(state, timeMeter, listener);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        StateWithConfiguration previousState = state;
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
            if (STATE_UPDATER.compareAndSet(this, previousState, newState)) {
                return toConsume;
            } else {
                previousState = state;
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        StateWithConfiguration previousState = state;
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens();
            if (tokensToConsume > availableToConsume) {
                return false;
            }
            newState.consume(tokensToConsume);
            if (STATE_UPDATER.compareAndSet(this, previousState, newState)) {
                return true;
            } else {
                previousState = state;
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected ConsumptionProbe tryConsumeAndReturnRemainingTokensImpl(long tokensToConsume) {
        StateWithConfiguration previousState = state;
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
            if (STATE_UPDATER.compareAndSet(this, previousState, newState)) {
                return ConsumptionProbe.consumed(availableToConsume - tokensToConsume);
            } else {
                previousState = state;
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected long reserveAndCalculateTimeToSleepImpl(long tokensToConsume, long waitIfBusyNanosLimit) {
        StateWithConfiguration previousState = state;
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            long nanosToCloseDeficit = newState.delayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);
            if (nanosToCloseDeficit == 0) {
                newState.consume(tokensToConsume);
                if (STATE_UPDATER.compareAndSet(this, previousState, newState)) {
                    return 0L;
                }
                previousState = state;
                newState.copyStateFrom(previousState);
                continue;
            }

            if (nanosToCloseDeficit == Long.MAX_VALUE || nanosToCloseDeficit > waitIfBusyNanosLimit) {
                return Long.MAX_VALUE;
            }

            newState.consume(tokensToConsume);
            if (STATE_UPDATER.compareAndSet(this, previousState, newState)) {
                return nanosToCloseDeficit;
            }
            previousState = state;
            newState.copyStateFrom(previousState);
        }
    }

    @Override
    protected void addTokensImpl(long tokensToAdd) {
        StateWithConfiguration previousState = state;
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(currentTimeNanos);
            newState.state.addTokens(newState.configuration.getBandwidths(), tokensToAdd);
            if (STATE_UPDATER.compareAndSet(this, previousState, newState)) {
                return;
            } else {
                previousState = state;
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected void replaceConfigurationImpl(BucketConfiguration newConfiguration) {
        StateWithConfiguration previousState = state;
        StateWithConfiguration newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            previousState.configuration.checkCompatibility(newConfiguration);
            newState.refillAllBandwidth(currentTimeNanos);
            newState.configuration = newConfiguration;
            if (STATE_UPDATER.compareAndSet(this, previousState, newState)) {
                return;
            } else {
                previousState = state;
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    public long getAvailableTokens() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        StateWithConfiguration snapshot = state.copy();
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
    protected CompletableFuture<Void> replaceConfigurationAsyncImpl(BucketConfiguration newConfiguration) {
        try {
            replaceConfigurationImpl(newConfiguration);
            return CompletableFuture.completedFuture(null);
        } catch (IncompatibleConfigurationException e) {
            CompletableFuture<Void> fail = new CompletableFuture<>();
            fail.completeExceptionally(e);
            return fail;
        }
    }

    @Override
    protected CompletableFuture<ConsumptionProbe> tryConsumeAndReturnRemainingTokensAsyncImpl(long tokensToConsume) {
        ConsumptionProbe result = tryConsumeAndReturnRemainingTokensImpl(tokensToConsume);
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
    public BucketState createSnapshot() {
        return state.state.copy();
    }

    @Override
    public BucketConfiguration getConfiguration() {
        return state.configuration;
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
                "state=" + STATE_UPDATER.get(this) +
                ", configuration=" + getConfiguration() +
                '}';
    }

}