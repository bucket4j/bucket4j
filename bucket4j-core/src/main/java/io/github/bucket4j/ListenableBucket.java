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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

public class ListenableBucket implements Bucket {

    private final BucketListener listener;
    private final Bucket target;

    public static Bucket decorate(Bucket target, BucketListener listener) {
        return new ListenableBucket(target, listener);
    }

    private ListenableBucket(Bucket target, BucketListener listener) {
        this.listener = Objects.requireNonNull(listener);
        this.target = Objects.requireNonNull(target);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return target.isAsyncModeSupported();
    }

    @Override
    public boolean tryConsume(long numTokens) {
        if (tryConsume(numTokens)) {
            listener.onConsumed(numTokens);
            return true;
        } else {
            listener.onRejected(numTokens);
            return false;
        }
    }

    @Override
    public ConsumptionProbe tryConsumeAndReturnRemaining(long numTokens) {
        ConsumptionProbe probe = target.tryConsumeAndReturnRemaining(numTokens);
        if (probe.isConsumed()) {
            listener.onConsumed(numTokens);
        } else {
            listener.onRejected(numTokens);
        }
        return probe;
    }

    @Override
    public long tryConsumeAsMuchAsPossible() {
        long consumed = target.tryConsumeAsMuchAsPossible();
        if (consumed > 0) {
            listener.onConsumed(consumed);
        }
        return consumed;
    }

    @Override
    public long tryConsumeAsMuchAsPossible(long limit) {
        long consumed = target.tryConsumeAsMuchAsPossible(limit);
        if (consumed > 0) {
            listener.onConsumed(consumed);
        }
        return consumed;
    }

    @Override
    public boolean tryConsume(long numTokens, long maxWaitTimeNanos, BlockingStrategy blockingStrategy) throws InterruptedException {
        boolean consumed;
        try {
            consumed = target.tryConsume(numTokens, maxWaitTimeNanos, new BlockingStrategy() {
                @Override
                public void park(long nanosToPark) throws InterruptedException {
                    blockingStrategy.park(nanosToPark);
                    listener.onParked(nanosToPark);
                }

                @Override
                public void parkUninterruptibly(long nanosToPark) {
                    // TODO consider to split BlockingStrategy by two independent interfaces
                    throw new IllegalStateException("should not be called");
                }
            });
        } catch (InterruptedException e) {
            listener.onInterrupted();
            listener.onRejected(numTokens);
            throw e;
        }
        if (consumed) {
            listener.onConsumed(numTokens);
        } else {
            listener.onRejected(numTokens);
        }
        return consumed;
    }

    @Override
    public boolean tryConsumeUninterruptibly(long numTokens, long maxWaitTimeNanos, BlockingStrategy blockingStrategy) {
        boolean consumed = target.tryConsumeUninterruptibly(numTokens, maxWaitTimeNanos, new BlockingStrategy() {
            @Override
            public void park(long nanosToPark) throws InterruptedException {
                // TODO consider to split BlockingStrategy by two independent interfaces
                throw new IllegalStateException("should not be called");
            }

            @Override
            public void parkUninterruptibly(long nanosToPark) {
                blockingStrategy.parkUninterruptibly(nanosToPark);
                listener.onParked(nanosToPark);
            }
        });

        if (consumed) {
            listener.onConsumed(numTokens);
        } else {
            listener.onRejected(numTokens);
        }
        return consumed;
    }

    @Override
    public void addTokens(long tokensToAdd) {
        target.addTokens(tokensToAdd);
    }

    @Override
    public long getAvailableTokens() {
        return target.getAvailableTokens();
    }

    @Override
    public void replaceConfiguration(BucketConfiguration newConfiguration) {
        target.replaceConfiguration(newConfiguration);
    }

    @Override
    public BucketState createSnapshot() {
        return target.createSnapshot();
    }

    private final AsyncBucket asyncView = new AsyncBucket() {
        @Override
        public CompletableFuture<Boolean> tryConsume(long numTokens) {
            return target.asAsync().tryConsume(numTokens).thenApply(consumed -> {
                if (consumed) {
                    listener.onConsumed(numTokens);
                } else {
                    listener.onRejected(numTokens);
                }
                return consumed;
            });
        }

        @Override
        public CompletableFuture<ConsumptionProbe> tryConsumeAndReturnRemaining(long numTokens) {
            return target.asAsync().tryConsumeAndReturnRemaining(numTokens).thenApply(probe -> {
                if (probe.isConsumed()) {
                    listener.onConsumed(numTokens);
                } else {
                    listener.onRejected(numTokens);
                }
                return probe;
            });
        }

        @Override
        public CompletableFuture<Long> tryConsumeAsMuchAsPossible() {
            return target.asAsync().tryConsumeAsMuchAsPossible().thenApply(consumedTokens -> {
                if (consumedTokens > 0) {
                    listener.onConsumed(consumedTokens);
                }
                return consumedTokens;
            });
        }

        @Override
        public CompletableFuture<Long> tryConsumeAsMuchAsPossible(long limit) {
            return target.asAsync().tryConsumeAsMuchAsPossible(limit).thenApply(consumedTokens -> {
                if (consumedTokens > 0) {
                    listener.onConsumed(consumedTokens);
                }
                return consumedTokens;
            });
        }

        @Override
        public CompletableFuture<Boolean> tryConsume(long numTokens, long maxWaitNanos, ScheduledExecutorService scheduler) {
            return target.asAsync().tryConsume(numTokens, maxWaitNanos, scheduler).thenApply(consumed -> {
                if (consumed) {
                    listener.onConsumed(numTokens);
                } else {
                    listener.onRejected(numTokens);
                }
                return consumed;
            });
        }

        @Override
        public CompletableFuture<Void> addTokens(long tokensToAdd) {
            return target.asAsync().addTokens(tokensToAdd);
        }

        @Override
        public CompletableFuture<Void> replaceConfiguration(BucketConfiguration newConfiguration) {
            return target.asAsync().replaceConfiguration(newConfiguration);
        }
    };

    @Override
    public AsyncBucket asAsync() {
        if (!isAsyncModeSupported()) {
            throw new UnsupportedOperationException();
        }
        return asyncView;
    }

}
