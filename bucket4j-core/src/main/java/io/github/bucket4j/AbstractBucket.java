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

public abstract class AbstractBucket implements Bucket {

    protected static final long UNSPECIFIED_WAITING_LIMIT = -1;

    protected abstract long consumeAsMuchAsPossibleImpl(long limit);

    protected abstract boolean tryConsumeImpl(long tokensToConsume);

    protected abstract ConsumptionProbe tryConsumeAndReturnRemainingTokensImpl(long tokensToConsume);

    protected abstract boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyNanos, boolean uninterruptibly, BlockingStrategy blockingStrategy) throws InterruptedException;

    protected abstract void addTokensImpl(long tokensToAdd);

    private final AsyncBucket asyncView;

    public AbstractBucket() {
        if (isAsyncModeSupported()) {
            asyncView = createAsyncView();
        } else {
            asyncView = null;
        }
    }

    @Override
    public AsyncBucket asAsync() throws UnsupportedOperationException {
        if (asyncView == null) {
            throw new UnsupportedOperationException();
        }
        return asyncView;
    }

    @Override
    public boolean tryConsume(long tokensToConsume) {
        if (tokensToConsume <= 0) {
            throw BucketExceptions.nonPositiveTokensToConsume(tokensToConsume);
        }
        return tryConsumeImpl(tokensToConsume);
    }

    @Override
    public void consume(long tokensToConsume, BlockingStrategy blockingStrategy) throws InterruptedException {
        if (tokensToConsume <= 0) {
            throw BucketExceptions.nonPositiveTokensToConsume(tokensToConsume);
        }
        consumeOrAwaitImpl(tokensToConsume, UNSPECIFIED_WAITING_LIMIT, false, blockingStrategy);
    }

    @Override
    public boolean consume(long tokensToConsume, long maxWaitTimeNanos, BlockingStrategy blockingStrategy) throws InterruptedException {
        if (tokensToConsume <= 0) {
            throw BucketExceptions.nonPositiveTokensToConsume(tokensToConsume);
        }

        if (maxWaitTimeNanos <= 0) {
            throw BucketExceptions.nonPositiveNanosToWait(maxWaitTimeNanos);
        }

        return consumeOrAwaitImpl(tokensToConsume, maxWaitTimeNanos, false, blockingStrategy);
    }

    @Override
    public void consumeUninterruptibly(long tokensToConsume, BlockingStrategy blockingStrategy) {
        if (tokensToConsume <= 0) {
            throw BucketExceptions.nonPositiveTokensToConsume(tokensToConsume);
        }

        try {
            consumeOrAwaitImpl(tokensToConsume, UNSPECIFIED_WAITING_LIMIT, true, blockingStrategy);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Should never come here", e);
        }
    }

    @Override
    public boolean consumeUninterruptibly(long tokensToConsume, long maxWaitTimeNanos, BlockingStrategy blockingStrategy) {
        if (tokensToConsume <= 0) {
            throw BucketExceptions.nonPositiveTokensToConsume(tokensToConsume);
        }

        if (maxWaitTimeNanos <= 0) {
            throw BucketExceptions.nonPositiveNanosToWait(maxWaitTimeNanos);
        }

        try {
            return consumeOrAwaitImpl(tokensToConsume, maxWaitTimeNanos, true, blockingStrategy);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Should never come here", e);
        }
    }

    @Override
    public long tryConsumeAsMuchAsPossible(long limit) {
        if (limit <= 0) {
            throw BucketExceptions.nonPositiveTokensToConsume(limit);
        }
        return consumeAsMuchAsPossibleImpl(limit);
    }

    @Override
    public long tryConsumeAsMuchAsPossible() {
        return consumeAsMuchAsPossibleImpl(Long.MAX_VALUE);
    }

    @Override
    public ConsumptionProbe tryConsumeAndReturnRemaining(long tokensToConsume) {
        if (tokensToConsume <= 0) {
            throw BucketExceptions.nonPositiveTokensToConsume(tokensToConsume);
        }
        return tryConsumeAndReturnRemainingTokensImpl(tokensToConsume);
    }

    @Override
    public void addTokens(long tokensToAdd) {
        if (tokensToAdd <= 0) {
            throw new IllegalArgumentException("tokensToAdd should be >= 0");
        }
        addTokensImpl(tokensToAdd);
    }


    private AsyncBucket createAsyncView() {
        return new AsyncBucket() {
            @Override
            public CompletableFuture<Boolean> tryConsume(long numTokens) {
                // TODO
                return null;
            }

            @Override
            public CompletableFuture<ConsumptionProbe> tryConsumeAndReturnRemaining(long numTokens) {
                // TODO
                return null;
            }

            @Override
            public CompletableFuture<Long> tryConsumeAsMuchAsPossible() {
                // TODO
                return null;
            }

            @Override
            public CompletableFuture<Long> tryConsumeAsMuchAsPossible(long limit) {
                // TODO
                return null;
            }

            @Override
            public CompletableFuture<Void> addTokens(long tokensToAdd) {
                // TODO
                return null;
            }

            @Override
            public CompletableFuture<Boolean> consume(long numTokens, long maxWaitTimeNanos, ScheduledExecutorService scheduler) throws InterruptedException {
                // TODO
                return null;
            }

            @Override
            public CompletableFuture<Void> consume(long numTokens, ScheduledExecutorService scheduler) throws InterruptedException {
                // TODO
                return null;
            }
        };
    }

}