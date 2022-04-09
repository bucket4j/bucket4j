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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SynchronizedBucket extends AbstractBucket implements LocalBucket {

    private BucketConfiguration configuration;
    private final TimeMeter timeMeter;
    private BucketState state;
    private final Lock lock;

    public SynchronizedBucket(BucketConfiguration configuration, MathType mathType, TimeMeter timeMeter) {
        this(configuration, mathType, timeMeter, new ReentrantLock());
    }

    SynchronizedBucket(BucketConfiguration configuration, MathType mathType, TimeMeter timeMeter, Lock lock) {
        this(BucketListener.NOPE, configuration, timeMeter, lock, BucketState.createInitialState(configuration, mathType, timeMeter.currentTimeNanos()));
    }

    private SynchronizedBucket(BucketListener listener, BucketConfiguration configuration, TimeMeter timeMeter, Lock lock, BucketState initialState) {
        super(listener);
        this.configuration = configuration;
        this.timeMeter = timeMeter;
        this.state = initialState;
        this.lock = lock;
    }

    public void setConfiguration(BucketConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Bucket toListenable(BucketListener listener) {
        return new SynchronizedBucket(listener, configuration, timeMeter, lock, state);
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(currentTimeNanos);
            long availableToConsume = state.getAvailableTokens();
            long toConsume = Math.min(limit, availableToConsume);
            if (toConsume == 0) {
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
            if (toConsume == 0) {
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
    public String toString() {
        synchronized (this) {
            return "SynchronizedBucket{" +
                "state=" + state +
                ", configuration=" + getConfiguration() +
                '}';
        }
    }

}
