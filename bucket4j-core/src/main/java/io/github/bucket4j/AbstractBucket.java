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

import static io.github.bucket4j.LimitChecker.*;

public abstract class AbstractBucket implements Bucket, BlockingBucket {

    protected static long INFINITY_DURATION = Long.MAX_VALUE;
    protected static long UNLIMITED_AMOUNT = Long.MAX_VALUE;

    protected abstract long consumeAsMuchAsPossibleImpl(long limit);

    protected abstract boolean tryConsumeImpl(long tokensToConsume);

    protected abstract ConsumptionProbe tryConsumeAndReturnRemainingTokensImpl(long tokensToConsume);

    protected abstract EstimationProbe estimateAbilityToConsumeImpl(long numTokens);

    protected abstract long reserveAndCalculateTimeToSleepImpl(long tokensToConsume, long waitIfBusyNanos);

    protected abstract void addTokensImpl(long tokensToAdd);

    protected abstract BucketConfiguration replaceConfigurationImpl(BucketConfiguration newConfiguration);

    protected abstract long consumeIgnoringRateLimitsImpl(long tokensToConsume);

    protected abstract VerboseResult<Long> consumeAsMuchAsPossibleVerboseImpl(long limit);

    protected abstract VerboseResult<Boolean> tryConsumeVerboseImpl(long tokensToConsume);

    protected abstract VerboseResult<ConsumptionProbe> tryConsumeAndReturnRemainingTokensVerboseImpl(long tokensToConsume);

    protected abstract VerboseResult<EstimationProbe> estimateAbilityToConsumeVerboseImpl(long numTokens);

    protected abstract VerboseResult<Long> getAvailableTokensVerboseImpl();

    protected abstract VerboseResult<Nothing> addTokensVerboseImpl(long tokensToAdd);

    protected abstract VerboseResult<BucketConfiguration> replaceConfigurationVerboseImpl(BucketConfiguration newConfiguration);

    protected abstract VerboseResult<Long> consumeIgnoringRateLimitsVerboseImpl(long tokensToConsume);

    public AbstractBucket(BucketListener listener) {
        if (listener == null) {
            throw BucketExceptions.nullListener();
        }
        this.listener = listener;
    }

    private final BucketListener listener;

    private final VerboseBucket verboseView = new VerboseBucket() {
        @Override
        public VerboseResult<Boolean> tryConsume(long tokensToConsume) {
            checkTokensToConsume(tokensToConsume);

            VerboseResult<Boolean> result = tryConsumeVerboseImpl(tokensToConsume);
            if (result.getValue()) {
                listener.onConsumed(tokensToConsume);
            } else {
                listener.onRejected(tokensToConsume);
            }

            return result;
        }

        @Override
        public VerboseResult<Long> consumeIgnoringRateLimits(long tokens) {
            checkTokensToConsume(tokens);
            VerboseResult<Long> result = consumeIgnoringRateLimitsVerboseImpl(tokens);
            long penaltyNanos = result.getValue();
            if (penaltyNanos == INFINITY_DURATION) {
                throw BucketExceptions.reservationOverflow();
            }
            listener.onConsumed(tokens);
            return result;
        }

        @Override
        public VerboseResult<ConsumptionProbe> tryConsumeAndReturnRemaining(long tokensToConsume) {
            checkTokensToConsume(tokensToConsume);

            VerboseResult<ConsumptionProbe> result = tryConsumeAndReturnRemainingTokensVerboseImpl(tokensToConsume);
            ConsumptionProbe probe = result.getValue();
            if (probe.isConsumed()) {
                listener.onConsumed(tokensToConsume);
            } else {
                listener.onRejected(tokensToConsume);
            }
            return result;
        }

        @Override
        public VerboseResult<EstimationProbe> estimateAbilityToConsume(long numTokens) {
            checkTokensToConsume(numTokens);
            return estimateAbilityToConsumeVerboseImpl(numTokens);
        }

        @Override
        public VerboseResult<Long> tryConsumeAsMuchAsPossible() {
            VerboseResult<Long> result = consumeAsMuchAsPossibleVerboseImpl(UNLIMITED_AMOUNT);
            long consumed = result.getValue();
            if (consumed > 0) {
                listener.onConsumed(consumed);
            }
            return result;
        }

        @Override
        public VerboseResult<Long> tryConsumeAsMuchAsPossible(long limit) {
            checkTokensToConsume(limit);

            VerboseResult<Long> result = consumeAsMuchAsPossibleVerboseImpl(limit);
            long consumed = result.getValue();
            if (consumed > 0) {
                listener.onConsumed(consumed);
            }

            return result;
        }

        @Override
        public VerboseResult<Long> getAvailableTokens() {
            return getAvailableTokensVerboseImpl();
        }

        @Override
        public VerboseResult<Nothing> addTokens(long tokensToAdd) {
            checkTokensToAdd(tokensToAdd);
            return addTokensVerboseImpl(tokensToAdd);
        }

        @Override
        public VerboseResult<Nothing> replaceConfiguration(BucketConfiguration newConfiguration) {
            checkConfiguration(newConfiguration);
            VerboseResult<BucketConfiguration> result = replaceConfigurationVerboseImpl(newConfiguration);

            return result.map(conflictingConfiguration -> {
                if (conflictingConfiguration != null) {
                    throw new IncompatibleConfigurationException(conflictingConfiguration, newConfiguration);
                }
                return Nothing.INSTANCE;
            });
        }
    };

    @Override
    public VerboseBucket asVerbose() {
        return verboseView;
    }

    @Override
    public BlockingBucket asBlocking() {
        return this;
    }

    @Override
    public boolean tryConsume(long tokensToConsume) {
        checkTokensToConsume(tokensToConsume);

        if (tryConsumeImpl(tokensToConsume)) {
            listener.onConsumed(tokensToConsume);
            return true;
        } else {
            listener.onRejected(tokensToConsume);
            return false;
        }
    }

    @Override
    public boolean tryConsume(long tokensToConsume, long maxWaitTimeNanos, BlockingStrategy blockingStrategy) throws InterruptedException {
        checkTokensToConsume(tokensToConsume);
        checkMaxWaitTime(maxWaitTimeNanos);

        long nanosToSleep = reserveAndCalculateTimeToSleepImpl(tokensToConsume, maxWaitTimeNanos);
        if (nanosToSleep == INFINITY_DURATION) {
            listener.onRejected(tokensToConsume);
            return false;
        }

        listener.onConsumed(tokensToConsume);
        if (nanosToSleep > 0L) {
            try {
                blockingStrategy.park(nanosToSleep);
            } catch (InterruptedException e) {
                listener.onInterrupted(e);
                throw e;
            }
            listener.onParked(nanosToSleep);
        }

        return true;
    }

    @Override
    public boolean tryConsumeUninterruptibly(long tokensToConsume, long maxWaitTimeNanos, UninterruptibleBlockingStrategy blockingStrategy) {
        checkTokensToConsume(tokensToConsume);
        checkMaxWaitTime(maxWaitTimeNanos);

        long nanosToSleep = reserveAndCalculateTimeToSleepImpl(tokensToConsume, maxWaitTimeNanos);
        if (nanosToSleep == INFINITY_DURATION) {
            listener.onRejected(tokensToConsume);
            return false;
        }

        listener.onConsumed(tokensToConsume);
        if (nanosToSleep > 0L) {
            blockingStrategy.parkUninterruptibly(nanosToSleep);
            listener.onParked(nanosToSleep);
        }

        return true;
    }

    @Override
    public void consume(long tokensToConsume, BlockingStrategy blockingStrategy) throws InterruptedException {
        checkTokensToConsume(tokensToConsume);

        long nanosToSleep = reserveAndCalculateTimeToSleepImpl(tokensToConsume, INFINITY_DURATION);
        if (nanosToSleep == INFINITY_DURATION) {
            throw BucketExceptions.reservationOverflow();
        }

        listener.onConsumed(tokensToConsume);
        if (nanosToSleep > 0L) {
            try {
                blockingStrategy.park(nanosToSleep);
            } catch (InterruptedException e) {
                listener.onInterrupted(e);
                throw e;
            }
            listener.onParked(nanosToSleep);
        }
    }

    @Override
    public void consumeUninterruptibly(long tokensToConsume, UninterruptibleBlockingStrategy blockingStrategy) {
        checkTokensToConsume(tokensToConsume);

        long nanosToSleep = reserveAndCalculateTimeToSleepImpl(tokensToConsume, INFINITY_DURATION);
        if (nanosToSleep == INFINITY_DURATION) {
            throw BucketExceptions.reservationOverflow();
        }

        listener.onConsumed(tokensToConsume);
        if (nanosToSleep > 0L) {
            blockingStrategy.parkUninterruptibly(nanosToSleep);
            listener.onParked(nanosToSleep);
        }
    }

    @Override
    public long consumeIgnoringRateLimits(long tokens) {
        checkTokensToConsume(tokens);
        long penaltyNanos = consumeIgnoringRateLimitsImpl(tokens);
        if (penaltyNanos == INFINITY_DURATION) {
            throw BucketExceptions.reservationOverflow();
        }
        listener.onConsumed(tokens);
        return penaltyNanos;
    }

    @Override
    public long tryConsumeAsMuchAsPossible(long limit) {
        checkTokensToConsume(limit);

        long consumed = consumeAsMuchAsPossibleImpl(limit);
        if (consumed > 0) {
            listener.onConsumed(consumed);
        }

        return consumed;
    }

    @Override
    public long tryConsumeAsMuchAsPossible() {
        long consumed = consumeAsMuchAsPossibleImpl(UNLIMITED_AMOUNT);
        if (consumed > 0) {
            listener.onConsumed(consumed);
        }
        return consumed;
    }

    @Override
    public ConsumptionProbe tryConsumeAndReturnRemaining(long tokensToConsume) {
        checkTokensToConsume(tokensToConsume);

        ConsumptionProbe probe = tryConsumeAndReturnRemainingTokensImpl(tokensToConsume);
        if (probe.isConsumed()) {
            listener.onConsumed(tokensToConsume);
        } else {
            listener.onRejected(tokensToConsume);
        }
        return probe;
    }

    @Override
    public EstimationProbe estimateAbilityToConsume(long numTokens) {
        checkTokensToConsume(numTokens);
        return estimateAbilityToConsumeImpl(numTokens);
    }

    @Override
    public void addTokens(long tokensToAdd) {
        checkTokensToAdd(tokensToAdd);
        addTokensImpl(tokensToAdd);
    }

    @Override
    public void replaceConfiguration(BucketConfiguration newConfiguration) {
        checkConfiguration(newConfiguration);
        BucketConfiguration conflictingConfiguration = replaceConfigurationImpl(newConfiguration);
        if (conflictingConfiguration != null) {
            throw new IncompatibleConfigurationException(conflictingConfiguration, newConfiguration);
        }
    }

    protected BucketListener getListener() {
        return listener;
    }

}
