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
    private Bandwidth[] bandwidths;
    private final TimeMeter timeMeter;
    private final BucketState state;
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
        this.bandwidths = configuration.getBandwidths();
        this.timeMeter = timeMeter;
        this.state = initialState;
        this.lock = lock;
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
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            long availableToConsume = state.getAvailableTokens(bandwidths);
            long toConsume = Math.min(limit, availableToConsume);
            if (toConsume == 0) {
                return 0;
            }
            state.consume(bandwidths, toConsume);
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
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            long availableToConsume = state.getAvailableTokens(bandwidths);
            if (tokensToConsume > availableToConsume) {
                return false;
            }
            state.consume(bandwidths, tokensToConsume);
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
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            long availableToConsume = state.getAvailableTokens(bandwidths);
            if (tokensToConsume > availableToConsume) {
                long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(bandwidths, tokensToConsume, currentTimeNanos);
                long nanosToWaitForReset = state.calculateFullRefillingTime(bandwidths, currentTimeNanos);
                return ConsumptionProbe.rejected(availableToConsume, nanosToWaitForRefill, nanosToWaitForReset);
            }
            state.consume(bandwidths, tokensToConsume);
            long remainingTokens = availableToConsume - tokensToConsume;
            long nanosToWaitForReset = state.calculateFullRefillingTime(bandwidths, currentTimeNanos);
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
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            long availableToConsume = state.getAvailableTokens(bandwidths);
            if (tokensToEstimate > availableToConsume) {
                long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(bandwidths, tokensToEstimate, currentTimeNanos);
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
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            long nanosToCloseDeficit = state.calculateDelayNanosAfterWillBePossibleToConsume(bandwidths, tokensToConsume, currentTimeNanos);

            if (nanosToCloseDeficit == Long.MAX_VALUE || nanosToCloseDeficit > waitIfBusyNanosLimit) {
                return Long.MAX_VALUE;
            }

            state.consume(bandwidths, tokensToConsume);
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
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            long nanosToCloseDeficit = state.calculateDelayNanosAfterWillBePossibleToConsume(bandwidths, tokensToConsume, currentTimeNanos);

            if (nanosToCloseDeficit == INFINITY_DURATION) {
                return nanosToCloseDeficit;
            }
            state.consume(bandwidths, tokensToConsume);
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
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            long availableToConsume = state.getAvailableTokens(bandwidths);
            long toConsume = Math.min(limit, availableToConsume);
            if (toConsume == 0) {
                return new VerboseResult<>(currentTimeNanos, 0L, configuration, state.copy());
            }
            state.consume(bandwidths, toConsume);
            return new VerboseResult<>(currentTimeNanos, toConsume, configuration, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<Boolean> tryConsumeVerboseImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            long availableToConsume = state.getAvailableTokens(bandwidths);
            if (tokensToConsume > availableToConsume) {
                return new VerboseResult<>(currentTimeNanos, false, configuration, state.copy());
            }
            state.consume(bandwidths, tokensToConsume);
            return new VerboseResult<>(currentTimeNanos, true, configuration, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<ConsumptionProbe> tryConsumeAndReturnRemainingTokensVerboseImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            long availableToConsume = state.getAvailableTokens(bandwidths);
            if (tokensToConsume > availableToConsume) {
                long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(bandwidths, tokensToConsume, currentTimeNanos);
                ConsumptionProbe probe = ConsumptionProbe.rejected(availableToConsume, nanosToWaitForRefill);
                return new VerboseResult<>(currentTimeNanos, probe, configuration, state.copy());
            }
            state.consume(bandwidths, tokensToConsume);
            ConsumptionProbe probe = ConsumptionProbe.consumed(availableToConsume - tokensToConsume);
            return new VerboseResult<>(currentTimeNanos, probe, configuration, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<EstimationProbe> estimateAbilityToConsumeVerboseImpl(long tokensToEstimate) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            long availableToConsume = state.getAvailableTokens(bandwidths);
            if (tokensToEstimate > availableToConsume) {
                long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(bandwidths, tokensToEstimate, currentTimeNanos);
                EstimationProbe estimationProbe = EstimationProbe.canNotBeConsumed(availableToConsume, nanosToWaitForRefill);
                return new VerboseResult<>(currentTimeNanos, estimationProbe, configuration, state.copy());
            }
            EstimationProbe estimationProbe = EstimationProbe.canBeConsumed(availableToConsume);
            return new VerboseResult<>(currentTimeNanos, estimationProbe, configuration, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<Long> getAvailableTokensVerboseImpl() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            long availableTokens = state.getAvailableTokens(bandwidths);
            return new VerboseResult<>(currentTimeNanos, availableTokens, configuration, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<Nothing> addTokensVerboseImpl(long tokensToAdd) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            state.addTokens(bandwidths, tokensToAdd);
            return new VerboseResult<>(currentTimeNanos, Nothing.INSTANCE, configuration, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<BucketConfiguration> replaceConfigurationVerboseImpl(BucketConfiguration newConfiguration) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            if (!configuration.isCompatible(newConfiguration)) {
                return new VerboseResult<>(currentTimeNanos, configuration, configuration, state.copy());
            }
            this.state.refillAllBandwidth(bandwidths, currentTimeNanos);
            this.configuration = newConfiguration;
            this.bandwidths = newConfiguration.getBandwidths();
            return new VerboseResult<>(currentTimeNanos, null, configuration, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected VerboseResult<Long> consumeIgnoringRateLimitsVerboseImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            long nanosToCloseDeficit = state.calculateDelayNanosAfterWillBePossibleToConsume(bandwidths, tokensToConsume, currentTimeNanos);

            if (nanosToCloseDeficit == INFINITY_DURATION) {
                return new VerboseResult<>(currentTimeNanos, nanosToCloseDeficit, configuration, state.copy());
            }
            state.consume(bandwidths, tokensToConsume);
            return new VerboseResult<>(currentTimeNanos, nanosToCloseDeficit, configuration, state.copy());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void addTokensImpl(long tokensToAdd) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            state.addTokens(bandwidths, tokensToAdd);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long getAvailableTokens() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            return state.getAvailableTokens(bandwidths);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected BucketConfiguration replaceConfigurationImpl(BucketConfiguration newConfiguration) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            if (!configuration.isCompatible(newConfiguration)) {
                return configuration;
            }
            this.state.refillAllBandwidth(bandwidths, currentTimeNanos);
            this.configuration = newConfiguration;
            this.bandwidths = newConfiguration.getBandwidths();
            return null;
        } finally {
            lock.unlock();
        }
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
    protected CompletableFuture<BucketConfiguration> replaceConfigurationAsyncImpl(BucketConfiguration newConfiguration) {
        BucketConfiguration result = replaceConfigurationImpl(newConfiguration);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    protected CompletableFuture<Long> consumeIgnoringRateLimitsAsyncImpl(long tokensToConsume) {
        long result = consumeIgnoringRateLimitsImpl(tokensToConsume);
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
        lock.lock();
        try {
            return state.copy();
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
