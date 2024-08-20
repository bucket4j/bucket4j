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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static io.github.bucket4j.LimitChecker.checkConfiguration;
import static io.github.bucket4j.LimitChecker.checkMaxWaitTime;
import static io.github.bucket4j.LimitChecker.checkMigrationMode;
import static io.github.bucket4j.LimitChecker.checkScheduler;
import static io.github.bucket4j.LimitChecker.checkTokensToAdd;
import static io.github.bucket4j.LimitChecker.checkTokensToConsume;

public abstract class AbstractBucket implements Bucket {

    protected static final long INFINITY_DURATION = Long.MAX_VALUE;
    protected static final long UNLIMITED_AMOUNT = Long.MAX_VALUE;

    protected abstract long consumeAsMuchAsPossibleImpl(long limit);

    protected abstract boolean tryConsumeImpl(long tokensToConsume);

    protected abstract ConsumptionProbe tryConsumeAndReturnRemainingTokensImpl(long tokensToConsume);

    protected abstract EstimationProbe estimateAbilityToConsumeImpl(long numTokens);

    protected abstract long reserveAndCalculateTimeToSleepImpl(long tokensToConsume, long waitIfBusyNanos);

    protected abstract void addTokensImpl(long tokensToAdd);

    protected abstract void forceAddTokensImpl(long tokensToAdd);

    protected abstract void replaceConfigurationImpl(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy);

    protected abstract long consumeIgnoringRateLimitsImpl(long tokensToConsume);

    protected abstract VerboseResult<Long> consumeAsMuchAsPossibleVerboseImpl(long limit);

    protected abstract VerboseResult<Boolean> tryConsumeVerboseImpl(long tokensToConsume);

    protected abstract VerboseResult<ConsumptionProbe> tryConsumeAndReturnRemainingTokensVerboseImpl(long tokensToConsume);

    protected abstract VerboseResult<EstimationProbe> estimateAbilityToConsumeVerboseImpl(long numTokens);

    protected abstract VerboseResult<Long> getAvailableTokensVerboseImpl();

    protected abstract VerboseResult<Nothing> addTokensVerboseImpl(long tokensToAdd);

    protected abstract VerboseResult<Nothing> forceAddTokensVerboseImpl(long tokensToAdd);

    protected abstract VerboseResult<Nothing> resetVerboseImpl();

    protected abstract VerboseResult<Nothing> replaceConfigurationVerboseImpl(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy);

    protected abstract VerboseResult<Long> consumeIgnoringRateLimitsVerboseImpl(long tokensToConsume);

    protected abstract VerboseResult<Long> reserveAndCalculateTimeToSleepVerboseImpl(long tokensToConsume, long maxWaitTimeNanos);

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
        public VerboseResult<Nothing> reset() {
            return resetVerboseImpl();
        }

        @Override
        public VerboseResult<Nothing> forceAddTokens(long tokensToAdd) {
            checkTokensToAdd(tokensToAdd);
            return forceAddTokensVerboseImpl(tokensToAdd);
        }

        @Override
        public VerboseResult<Nothing> replaceConfiguration(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
            checkConfiguration(newConfiguration);
            checkMigrationMode(tokensInheritanceStrategy);

            return replaceConfigurationVerboseImpl(newConfiguration, tokensInheritanceStrategy);
        }
    };

    private final BlockingBucket blockingView = new BlockingBucket() {
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
                    listener.beforeParking(nanosToSleep);
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
                listener.beforeParking(nanosToSleep);
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
                    listener.beforeParking(nanosToSleep);
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
                listener.beforeParking(nanosToSleep);
                blockingStrategy.parkUninterruptibly(nanosToSleep);
                listener.onParked(nanosToSleep);
            }
        }

        @Override
        public VerboseBlockingBucket asVerbose() {
            return verboseBlockingView;
        }
    };

    private final SchedulingBucket schedulingBucketView = new SchedulingBucket() {
        @Override
        public CompletableFuture<Boolean> tryConsume(long tokensToConsume, long maxWaitTimeNanos, ScheduledExecutorService scheduler) {
            checkMaxWaitTime(maxWaitTimeNanos);
            checkTokensToConsume(tokensToConsume);
            checkScheduler(scheduler);

            try {
                long nanosToSleep = reserveAndCalculateTimeToSleepImpl(tokensToConsume, maxWaitTimeNanos);
                if (nanosToSleep == INFINITY_DURATION) {
                    listener.onRejected(tokensToConsume);
                    return CompletableFuture.completedFuture(false);
                }
                if (nanosToSleep == 0L) {
                    listener.onConsumed(tokensToConsume);
                    return CompletableFuture.completedFuture(true);
                }

                listener.onConsumed(tokensToConsume);
                listener.onDelayed(nanosToSleep);
                CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
                Runnable delayedCompletion = () -> resultFuture.complete(true);
                scheduler.schedule(delayedCompletion, nanosToSleep, TimeUnit.NANOSECONDS);
                return resultFuture;
            } catch (Throwable t) {
                return failedFuture(t);
            }
        }

        @Override
        public CompletableFuture<Void> consume(long tokensToConsume, ScheduledExecutorService scheduler) {
            checkTokensToConsume(tokensToConsume);
            checkScheduler(scheduler);

            try {
                long nanosToSleep = reserveAndCalculateTimeToSleepImpl(tokensToConsume, INFINITY_DURATION);
                if (nanosToSleep == INFINITY_DURATION) {
                    return failedFuture(BucketExceptions.reservationOverflow());
                }
                if (nanosToSleep == 0L) {
                    listener.onConsumed(tokensToConsume);
                    return CompletableFuture.completedFuture(null);
                }

                listener.onConsumed(tokensToConsume);
                listener.onDelayed(nanosToSleep);
                CompletableFuture<Void> resultFuture = new CompletableFuture<>();
                Runnable delayedCompletion = () -> resultFuture.complete(null);
                scheduler.schedule(delayedCompletion, nanosToSleep, TimeUnit.NANOSECONDS);
                return resultFuture;
            } catch (Throwable t) {
                return failedFuture(t);
            }
        }

        @Override
        public VerboseSchedulingBucket asVerbose() {
            return verboseSchedulingView;
        }
    };

    private final VerboseSchedulingBucket verboseSchedulingView = new VerboseSchedulingBucket() {

        @Override
        public CompletableFuture<VerboseResult<Boolean>> tryConsume(long tokensToConsume, long maxWaitTimeNanos, ScheduledExecutorService scheduler) {
            checkMaxWaitTime(maxWaitTimeNanos);
            checkTokensToConsume(tokensToConsume);
            checkScheduler(scheduler);

            try {
                VerboseResult<Long> nanosToSleepVerbose = reserveAndCalculateTimeToSleepVerboseImpl(tokensToConsume, maxWaitTimeNanos);
                long nanosToSleep = nanosToSleepVerbose.getValue();
                if (nanosToSleep == INFINITY_DURATION) {
                    listener.onRejected(tokensToConsume);
                    return CompletableFuture.completedFuture(nanosToSleepVerbose.withValue(false));
                }
                if (nanosToSleep == 0L) {
                    listener.onConsumed(tokensToConsume);
                    return CompletableFuture.completedFuture(nanosToSleepVerbose.withValue(true));
                }

                listener.onConsumed(tokensToConsume);
                listener.onDelayed(nanosToSleep);
                CompletableFuture<VerboseResult<Boolean>> resultFuture = new CompletableFuture<>();
                Runnable delayedCompletion = () -> resultFuture.complete(nanosToSleepVerbose.withValue(true));
                scheduler.schedule(delayedCompletion, nanosToSleep, TimeUnit.NANOSECONDS);
                return resultFuture;
            } catch (Throwable t) {
                return failedFuture(t);
            }
        }

        @Override
        public CompletableFuture<VerboseResult<Void>> consume(long tokensToConsume, ScheduledExecutorService scheduler) {
            checkTokensToConsume(tokensToConsume);
            checkScheduler(scheduler);

            try {
                VerboseResult<Long> nanosToSleepVerbose = reserveAndCalculateTimeToSleepVerboseImpl(tokensToConsume, INFINITY_DURATION);
                long nanosToSleep = nanosToSleepVerbose.getValue();
                if (nanosToSleep == INFINITY_DURATION) {
                    return failedFuture(BucketExceptions.reservationOverflow());
                }
                if (nanosToSleep == 0L) {
                    listener.onConsumed(tokensToConsume);
                    return CompletableFuture.completedFuture(nanosToSleepVerbose.withValue(null));
                }

                listener.onConsumed(tokensToConsume);
                listener.onDelayed(nanosToSleep);
                CompletableFuture<VerboseResult<Void>> resultFuture = new CompletableFuture<>();
                Runnable delayedCompletion = () -> resultFuture.complete(nanosToSleepVerbose.withValue(null));
                scheduler.schedule(delayedCompletion, nanosToSleep, TimeUnit.NANOSECONDS);
                return resultFuture;
            } catch (Throwable t) {
                return failedFuture(t);
            }
        }
    };

    private final VerboseBlockingBucket verboseBlockingView = new VerboseBlockingBucket() {

        @Override
        public VerboseResult<Boolean> tryConsume(long tokensToConsume, long maxWaitTimeNanos, BlockingStrategy blockingStrategy) throws InterruptedException {
            checkTokensToConsume(tokensToConsume);
            checkMaxWaitTime(maxWaitTimeNanos);

            VerboseResult<Long> nanosToSleepVerbose = reserveAndCalculateTimeToSleepVerboseImpl(tokensToConsume, maxWaitTimeNanos);
            long nanosToSleep = nanosToSleepVerbose.getValue();
            if (nanosToSleep == INFINITY_DURATION) {
                listener.onRejected(tokensToConsume);
                return nanosToSleepVerbose.withValue(false);
            }

            listener.onConsumed(tokensToConsume);
            if (nanosToSleep > 0L) {
                try {
                    listener.beforeParking(nanosToSleep);
                    blockingStrategy.park(nanosToSleep);
                } catch (InterruptedException e) {
                    listener.onInterrupted(e);
                    throw e;
                }
                listener.onParked(nanosToSleep);
            }

            return nanosToSleepVerbose.withValue(true);
        }

        @Override
        public VerboseResult<Boolean> tryConsumeUninterruptibly(long tokensToConsume, long maxWaitTimeNanos, UninterruptibleBlockingStrategy blockingStrategy) {
            checkTokensToConsume(tokensToConsume);
            checkMaxWaitTime(maxWaitTimeNanos);

            VerboseResult<Long> nanosToSleepVerbose = reserveAndCalculateTimeToSleepVerboseImpl(tokensToConsume, maxWaitTimeNanos);
            long nanosToSleep = nanosToSleepVerbose.getValue();
            if (nanosToSleep == INFINITY_DURATION) {
                listener.onRejected(tokensToConsume);
                return nanosToSleepVerbose.withValue(false);
            }

            listener.onConsumed(tokensToConsume);
            if (nanosToSleep > 0L) {
                listener.beforeParking(nanosToSleep);
                blockingStrategy.parkUninterruptibly(nanosToSleep);
                listener.onParked(nanosToSleep);
            }

            return nanosToSleepVerbose.withValue(true);
        }

        @Override
        public VerboseResult<Void> consume(long tokensToConsume, BlockingStrategy blockingStrategy) throws InterruptedException {
            checkTokensToConsume(tokensToConsume);

            VerboseResult<Long> nanosToSleepVerbose = reserveAndCalculateTimeToSleepVerboseImpl(tokensToConsume, INFINITY_DURATION);
            long nanosToSleep = nanosToSleepVerbose.getValue();
            if (nanosToSleep == INFINITY_DURATION) {
                throw BucketExceptions.reservationOverflow();
            }

            listener.onConsumed(tokensToConsume);
            if (nanosToSleep > 0L) {
                try {
                    listener.beforeParking(nanosToSleep);
                    blockingStrategy.park(nanosToSleep);
                } catch (InterruptedException e) {
                    listener.onInterrupted(e);
                    throw e;
                }
                listener.onParked(nanosToSleep);
            }
            return nanosToSleepVerbose.withValue(null);
        }

        @Override
        public VerboseResult<Void> consumeUninterruptibly(long tokensToConsume, UninterruptibleBlockingStrategy blockingStrategy) {
            checkTokensToConsume(tokensToConsume);

            VerboseResult<Long> nanosToSleepVerbose = reserveAndCalculateTimeToSleepVerboseImpl(tokensToConsume, INFINITY_DURATION);
            long nanosToSleep = nanosToSleepVerbose.getValue();
            if (nanosToSleep == INFINITY_DURATION) {
                throw BucketExceptions.reservationOverflow();
            }

            listener.onConsumed(tokensToConsume);
            if (nanosToSleep > 0L) {
                listener.beforeParking(nanosToSleep);
                blockingStrategy.parkUninterruptibly(nanosToSleep);
                listener.onParked(nanosToSleep);
            }
            return nanosToSleepVerbose.withValue(null);
        }
    };

    @Override
    public SchedulingBucket asScheduler() {
        return schedulingBucketView;
    }

    @Override
    public VerboseBucket asVerbose() {
        return verboseView;
    }

    @Override
    public BlockingBucket asBlocking() {
        return blockingView;
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
    public void forceAddTokens(long tokensToAdd) {
        checkTokensToAdd(tokensToAdd);
        forceAddTokensImpl(tokensToAdd);
    }

    @Override
    public void replaceConfiguration(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        checkConfiguration(newConfiguration);
        checkMigrationMode(tokensInheritanceStrategy);
        replaceConfigurationImpl(newConfiguration, tokensInheritanceStrategy);
    }

    protected BucketListener getListener() {
        return listener;
    }

    public static <T> CompletableFuture<T> completedFuture(Supplier<T> supplier) {
        try {
            return CompletableFuture.completedFuture(supplier.get());
        } catch (Throwable t) {
            CompletableFuture<T> fail = new CompletableFuture<>();
            fail.completeExceptionally(t);
            return fail;
        }
    }

    public static <T> CompletableFuture<T> failedFuture(Throwable t) {
        CompletableFuture<T> fail = new CompletableFuture<>();
        fail.completeExceptionally(t);
        return fail;
    }

}
