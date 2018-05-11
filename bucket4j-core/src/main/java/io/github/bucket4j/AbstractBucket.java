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

package io.github.bucket4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AbstractBucket implements Bucket, BlockingBucket {

    private static long INFINITY_DURATION = Long.MAX_VALUE;
    private static long UNLIMITED_AMOUNT = Long.MAX_VALUE;

    protected abstract long consumeAsMuchAsPossibleImpl(long limit);

    protected abstract boolean tryConsumeImpl(long tokensToConsume);

    protected abstract ConsumptionProbe tryConsumeAndReturnRemainingTokensImpl(long tokensToConsume);

    protected abstract long reserveAndCalculateTimeToSleepImpl(long tokensToConsume, long waitIfBusyNanos);

    protected abstract void addTokensImpl(long tokensToAdd);

    protected abstract CompletableFuture<Long> tryConsumeAsMuchAsPossibleAsyncImpl(long limit);

    protected abstract CompletableFuture<Boolean> tryConsumeAsyncImpl(long tokensToConsume);

    protected abstract CompletableFuture<ConsumptionProbe> tryConsumeAndReturnRemainingTokensAsyncImpl(long tokensToConsume);

    protected abstract CompletableFuture<Long> reserveAndCalculateTimeToSleepAsyncImpl(long tokensToConsume, long maxWaitTimeNanos);

    protected abstract CompletableFuture<Void> addTokensAsyncImpl(long tokensToAdd);

    protected abstract void replaceConfigurationImpl(BucketConfiguration newConfiguration);

    protected abstract CompletableFuture<Void> replaceConfigurationAsyncImpl(BucketConfiguration newConfiguration);

    private final AsyncScheduledBucketImpl asyncView;
    private final BucketListener listener;

    public AbstractBucket(BucketListener listener) {
        if (listener == null) {
            throw BucketExceptions.nullListener();
        }

        this.listener = listener;
        this.asyncView = new AsyncScheduledBucketImpl() {
            @Override
            public CompletableFuture<Boolean> tryConsume(long tokensToConsume) {
                checkTokensToConsume(tokensToConsume);

                return tryConsumeAsyncImpl(tokensToConsume).thenApply(consumed -> {
                    if (consumed) {
                        listener.onConsumed(tokensToConsume);
                    } else {
                        listener.onRejected(tokensToConsume);
                    }
                    return consumed;
                });
            }

            @Override
            public CompletableFuture<ConsumptionProbe> tryConsumeAndReturnRemaining(long tokensToConsume) {
                checkTokensToConsume(tokensToConsume);

                return tryConsumeAndReturnRemainingTokensAsyncImpl(tokensToConsume).thenApply(probe -> {
                    if (probe.isConsumed()) {
                        listener.onConsumed(tokensToConsume);
                    } else {
                        listener.onRejected(tokensToConsume);
                    }
                    return probe;
                });
            }

            @Override
            public CompletableFuture<Long> tryConsumeAsMuchAsPossible() {
                return tryConsumeAsMuchAsPossibleAsyncImpl(UNLIMITED_AMOUNT).thenApply(consumedTokens -> {
                    if (consumedTokens > 0) {
                        listener.onConsumed(consumedTokens);
                    }
                    return consumedTokens;
                });
            }

            @Override
            public CompletableFuture<Long> tryConsumeAsMuchAsPossible(long limit) {
                checkTokensToConsume(limit);

                return tryConsumeAsMuchAsPossibleAsyncImpl(limit).thenApply(consumedTokens -> {
                    if (consumedTokens > 0) {
                        listener.onConsumed(consumedTokens);
                    }
                    return consumedTokens;
                });
            }

            @Override
            public CompletableFuture<Boolean> tryConsume(long tokensToConsume, long maxWaitTimeNanos, ScheduledExecutorService scheduler) {
                checkMaxWaitTime(maxWaitTimeNanos);
                checkTokensToConsume(tokensToConsume);
                checkScheduler(scheduler);
                CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
                CompletableFuture<Long> reservationFuture = reserveAndCalculateTimeToSleepAsyncImpl(tokensToConsume, maxWaitTimeNanos);
                reservationFuture.whenComplete((nanosToSleep, exception) -> {
                    if (exception != null) {
                        resultFuture.completeExceptionally(exception);
                        return;
                    }
                    if (nanosToSleep == INFINITY_DURATION) {
                        resultFuture.complete(false);
                        listener.onRejected(tokensToConsume);
                        return;
                    }
                    if (nanosToSleep == 0L) {
                        resultFuture.complete(true);
                        listener.onConsumed(tokensToConsume);
                        return;
                    }
                    try {
                        listener.onConsumed(tokensToConsume);
                        listener.onDelayed(nanosToSleep);
                        Runnable delayedCompletion = () -> resultFuture.complete(true);
                        scheduler.schedule(delayedCompletion, nanosToSleep, TimeUnit.NANOSECONDS);
                    } catch (Throwable t) {
                        resultFuture.completeExceptionally(t);
                    }
                });
                return resultFuture;
            }

            @Override
            public CompletableFuture<Void> consume(long tokensToConsume, ScheduledExecutorService scheduler) {
                checkTokensToConsume(tokensToConsume);
                checkScheduler(scheduler);
                CompletableFuture<Void> resultFuture = new CompletableFuture<>();
                CompletableFuture<Long> reservationFuture = reserveAndCalculateTimeToSleepAsyncImpl(tokensToConsume, INFINITY_DURATION);
                reservationFuture.whenComplete((nanosToSleep, exception) -> {
                    if (exception != null) {
                        resultFuture.completeExceptionally(exception);
                        return;
                    }
                    if (nanosToSleep == INFINITY_DURATION) {
                        String msg = "Existed hardware is unable to service the reservation of so many tokens";
                        resultFuture.completeExceptionally(new IllegalStateException(msg));
                        return;
                    }
                    if (nanosToSleep == 0L) {
                        resultFuture.complete(null);
                        listener.onConsumed(tokensToConsume);
                        return;
                    }
                    try {
                        listener.onConsumed(tokensToConsume);
                        listener.onDelayed(nanosToSleep);
                        Runnable delayedCompletion = () -> resultFuture.complete(null);
                        scheduler.schedule(delayedCompletion, nanosToSleep, TimeUnit.NANOSECONDS);
                    } catch (Throwable t) {
                        resultFuture.completeExceptionally(t);
                    }
                });
                return resultFuture;
            }

            @Override
            public CompletableFuture<Void> replaceConfiguration(BucketConfiguration newConfiguration) {
                checkConfiguration(newConfiguration);
                return replaceConfigurationAsyncImpl(newConfiguration);
            }

            @Override
            public CompletableFuture<Void> addTokens(long tokensToAdd) {
                checkTokensToAdd(tokensToAdd);
                return addTokensAsyncImpl(tokensToAdd);
            }

        };
    }

    @Override
    public AsyncBucket asAsync() {
        if (!isAsyncModeSupported()) {
            throw new UnsupportedOperationException();
        }
        return asyncView;
    }

    @Override
    public AsyncScheduledBucket asAsyncScheduler() {
        if (!isAsyncModeSupported()) {
            throw new UnsupportedOperationException();
        }
        return asyncView;
    }

    @Override
    public BlockingBucket asScheduler() {
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
            throw new IllegalStateException("Existed hardware is unable to service the reservation of so many tokens");
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
            throw new IllegalStateException("Existed hardware is unable to service the reservation of so many tokens");
        }

        listener.onConsumed(tokensToConsume);
        if (nanosToSleep > 0L) {
            blockingStrategy.parkUninterruptibly(nanosToSleep);
            listener.onParked(nanosToSleep);
        }
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
    public void addTokens(long tokensToAdd) {
        checkTokensToAdd(tokensToAdd);
        addTokensImpl(tokensToAdd);
    }

    @Override
    public void replaceConfiguration(BucketConfiguration newConfiguration) {
        checkConfiguration(newConfiguration);
        replaceConfigurationImpl(newConfiguration);
    }

    private static void checkTokensToAdd(long tokensToAdd) {
        if (tokensToAdd <= 0) {
            throw new IllegalArgumentException("tokensToAdd should be >= 0");
        }
    }

    private static void checkTokensToConsume(long tokensToConsume) {
        if (tokensToConsume <= 0) {
            throw BucketExceptions.nonPositiveTokensToConsume(tokensToConsume);
        }
    }

    private static void checkMaxWaitTime(long maxWaitTimeNanos) {
        if (maxWaitTimeNanos <= 0) {
            throw BucketExceptions.nonPositiveNanosToWait(maxWaitTimeNanos);
        }
    }

    private static void checkScheduler(ScheduledExecutorService scheduler) {
        if (scheduler == null) {
            throw BucketExceptions.nullScheduler();
        }
    }

    private static void checkConfiguration(BucketConfiguration newConfiguration) {
        if (newConfiguration == null) {
            throw BucketExceptions.nullConfiguration();
        }
    }

    private interface AsyncScheduledBucketImpl extends AsyncBucket, AsyncScheduledBucket {}

}