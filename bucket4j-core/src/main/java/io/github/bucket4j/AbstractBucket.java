/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AbstractBucket implements Bucket {

    protected abstract long consumeAsMuchAsPossibleImpl(long limit);

    protected abstract boolean tryConsumeImpl(long tokensToConsume);

    protected abstract ConsumptionProbe tryConsumeAndReturnRemainingTokensImpl(long tokensToConsume);

    protected abstract long reserveAndCalculateTimeToSleepImpl(long tokensToConsume, long waitIfBusyNanos);

    protected abstract void addTokensImpl(long tokensToAdd);

    /**
     * @param limit
     * @return
     * @throws UnsupportedOperationException if bucket does not support asynchronous mode
     */
    protected abstract CompletableFuture<Long> tryConsumeAsMuchAsPossibleAsyncImpl(long limit);

    /**
     * @param tokensToConsume
     * @return
     * @throws UnsupportedOperationException if bucket does not support asynchronous mode
     */
    protected abstract CompletableFuture<Boolean> tryConsumeAsyncImpl(long tokensToConsume);

    /**
     * @param tokensToConsume
     * @return
     * @throws UnsupportedOperationException if bucket does not support asynchronous mode
     */
    protected abstract CompletableFuture<ConsumptionProbe> tryConsumeAndReturnRemainingTokensAsyncImpl(long tokensToConsume);

    /**
     * @param tokensToConsume
     * @param maxWaitTimeNanos
     * @return
     * @throws UnsupportedOperationException if bucket does not support asynchronous mode
     */
    protected abstract CompletableFuture<Long> reserveAndCalculateTimeToSleepAsyncImpl(long tokensToConsume, long maxWaitTimeNanos);

    /**
     * @throws UnsupportedOperationException if bucket does not support asynchronous mode
     * @param tokensToAdd
     * @return
     */
    protected abstract CompletableFuture<Void> addTokensAsyncImpl(long tokensToAdd);

    protected abstract void replaceConfigurationImpl(BucketConfiguration newConfiguration);

    protected abstract CompletableFuture<Void> replaceConfigurationAsyncImpl(BucketConfiguration newConfiguration);

    private final AsyncBucket asyncView;

    public AbstractBucket() {
        asyncView = new AsyncBucket() {
            @Override
            public CompletableFuture<Boolean> tryConsume(long tokensToConsume) {
                checkTokensToConsume(tokensToConsume);
                return tryConsumeAsyncImpl(tokensToConsume);
            }

            @Override
            public CompletableFuture<ConsumptionProbe> tryConsumeAndReturnRemaining(long tokensToConsume) {
                checkTokensToConsume(tokensToConsume);
                return tryConsumeAndReturnRemainingTokensAsyncImpl(tokensToConsume);
            }

            @Override
            public CompletableFuture<Long> tryConsumeAsMuchAsPossible() {
                return tryConsumeAsMuchAsPossibleAsyncImpl(Long.MAX_VALUE);
            }

            @Override
            public CompletableFuture<Long> tryConsumeAsMuchAsPossible(long limit) {
                checkTokensToConsume(limit);
                return tryConsumeAsMuchAsPossibleAsyncImpl(limit);
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
                    if (nanosToSleep == Long.MAX_VALUE) {
                        resultFuture.complete(false);
                        return;
                    }
                    if (nanosToSleep == 0L) {
                        resultFuture.complete(true);
                        return;
                    }
                    try {
                        Runnable delayedCompletion = () -> resultFuture.complete(true);
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
    public boolean tryConsume(long tokensToConsume) {
        checkTokensToConsume(tokensToConsume);
        return tryConsumeImpl(tokensToConsume);
    }

    @Override
    public boolean tryConsume(long tokensToConsume, long maxWaitTimeNanos, BlockingStrategy blockingStrategy) throws InterruptedException {
        checkTokensToConsume(tokensToConsume);
        checkMaxWaitTime(maxWaitTimeNanos);

        long nanosToSleep = reserveAndCalculateTimeToSleepImpl(tokensToConsume, maxWaitTimeNanos);
        if (nanosToSleep == Long.MAX_VALUE) {
            return false;
        }
        if (nanosToSleep > 0L) {
            blockingStrategy.park(nanosToSleep);
        }
        return true;
    }

    @Override
    public boolean tryConsumeUninterruptibly(long tokensToConsume, long maxWaitTimeNanos, BlockingStrategy blockingStrategy) {
        checkTokensToConsume(tokensToConsume);
        checkMaxWaitTime(maxWaitTimeNanos);

        long nanosToSleep = reserveAndCalculateTimeToSleepImpl(tokensToConsume, maxWaitTimeNanos);
        if (nanosToSleep == Long.MAX_VALUE) {
            return false;
        }
        if (nanosToSleep > 0L) {
            blockingStrategy.parkUninterruptibly(nanosToSleep);
        }
        return true;
    }

    @Override
    public long tryConsumeAsMuchAsPossible(long limit) {
        checkTokensToConsume(limit);
        return consumeAsMuchAsPossibleImpl(limit);
    }

    @Override
    public long tryConsumeAsMuchAsPossible() {
        return consumeAsMuchAsPossibleImpl(Long.MAX_VALUE);
    }

    @Override
    public ConsumptionProbe tryConsumeAndReturnRemaining(long tokensToConsume) {
        checkTokensToConsume(tokensToConsume);
        return tryConsumeAndReturnRemainingTokensImpl(tokensToConsume);
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

}