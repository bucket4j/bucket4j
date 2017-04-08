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

import static io.github.bucket4j.BucketExceptions.nonPositiveNanosToWait;
import static io.github.bucket4j.BucketExceptions.nonPositiveTokensToConsume;

public abstract class AbstractBucket implements Bucket {

    protected static final long UNSPECIFIED_WAITING_LIMIT = -1;

    protected final BucketConfiguration configuration;
    protected final Bandwidth[] bandwidths;
    protected final TimeMeter timeMeter;

    protected AbstractBucket(BucketConfiguration configuration) {
        this.configuration = configuration;
        this.bandwidths = configuration.getBandwidths();
        this.timeMeter = configuration.getTimeMeter();
    }

    protected abstract long consumeAsMuchAsPossibleImpl(long limit);

    protected abstract boolean tryConsumeImpl(long tokensToConsume);

    protected abstract boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyNanos, boolean uninterruptibly) throws InterruptedException;

    protected abstract void addTokensImpl(long tokensToAdd);

    @Override
    public boolean tryConsume(long tokensToConsume) {
        if (tokensToConsume <= 0) {
            throw nonPositiveTokensToConsume(tokensToConsume);
        }
        return tryConsumeImpl(tokensToConsume);
    }

    @Override
    public void consume(long tokensToConsume) throws InterruptedException {
        if (tokensToConsume <= 0) {
            throw nonPositiveTokensToConsume(tokensToConsume);
        }
        consumeOrAwaitImpl(tokensToConsume, UNSPECIFIED_WAITING_LIMIT, false);
    }

    @Override
    public boolean consume(long tokensToConsume, long maxWaitTimeNanos) throws InterruptedException {
        if (tokensToConsume <= 0) {
            throw nonPositiveTokensToConsume(tokensToConsume);
        }

        if (maxWaitTimeNanos <= 0) {
            throw nonPositiveNanosToWait(maxWaitTimeNanos);
        }

        return consumeOrAwaitImpl(tokensToConsume, maxWaitTimeNanos, false);
    }

    @Override
    public void consumeUninterruptibly(long tokensToConsume) {
        if (tokensToConsume <= 0) {
            throw nonPositiveTokensToConsume(tokensToConsume);
        }

        try {
            consumeOrAwaitImpl(tokensToConsume, UNSPECIFIED_WAITING_LIMIT, true);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Should never come here", e);
        }
    }

    @Override
    public boolean consumeUninterruptibly(long tokensToConsume, long maxWaitTimeNanos) {
        if (tokensToConsume <= 0) {
            throw nonPositiveTokensToConsume(tokensToConsume);
        }

        if (maxWaitTimeNanos <= 0) {
            throw nonPositiveNanosToWait(maxWaitTimeNanos);
        }

        try {
            return consumeOrAwaitImpl(tokensToConsume, maxWaitTimeNanos, true);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Should never come here", e);
        }
    }

    @Override
    public long tryConsumeAsMuchAsPossible(long limit) {
        if (limit <= 0) {
            throw nonPositiveTokensToConsume(limit);
        }
        return consumeAsMuchAsPossibleImpl(limit);
    }

    @Override
    public long tryConsumeAsMuchAsPossible() {
        return consumeAsMuchAsPossibleImpl(Long.MAX_VALUE);
    }

    @Override
    public void addTokens(long tokensToAdd) {
        if (tokensToAdd <= 0) {
            throw new IllegalArgumentException("tokensToAdd should be >= 0");
        }
        addTokensImpl(tokensToAdd);
    }

    @Override
    public BucketConfiguration getConfiguration() {
        return configuration;
    }

}