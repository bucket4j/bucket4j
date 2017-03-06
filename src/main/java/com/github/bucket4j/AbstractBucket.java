/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.bucket4j;

import static com.github.bucket4j.BucketExceptions.nonPositiveNanosToWait;
import static com.github.bucket4j.BucketExceptions.nonPositiveTokensToConsume;

public abstract class AbstractBucket implements Bucket {

    protected static final long UNSPECIFIED_WAITING_LIMIT = -1;
    protected final BucketConfiguration configuration;

    protected AbstractBucket(BucketConfiguration configuration) {
        this.configuration = configuration;
    }

    protected abstract long consumeAsMuchAsPossibleImpl(long limit);

    protected abstract boolean tryConsumeImpl(long tokensToConsume);

    protected abstract boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyNanos) throws InterruptedException;

    @Override
    public boolean tryConsumeSingleToken() {
        return tryConsumeImpl(1);
    }

    @Override
    public boolean tryConsume(long tokensToConsume) {
        if (tokensToConsume <= 0) {
            throw nonPositiveTokensToConsume(tokensToConsume);
        }
        return tryConsumeImpl(tokensToConsume);
    }

    @Override
    public void consumeSingleToken() throws InterruptedException {
        consume(1);
    }

    @Override
    public void consume(long tokensToConsume) throws InterruptedException {
        if (tokensToConsume <= 0) {
            throw nonPositiveTokensToConsume(tokensToConsume);
        }
        consumeOrAwaitImpl(tokensToConsume, UNSPECIFIED_WAITING_LIMIT);
    }

    @Override
    public boolean consumeSingleToken(long maxWaitTime) throws InterruptedException {
        return consume(1, maxWaitTime);
    }

    @Override
    public boolean consume(long tokensToConsume, long maxWaitTime) throws InterruptedException {
        if (tokensToConsume <= 0) {
            throw nonPositiveTokensToConsume(tokensToConsume);
        }

        if (maxWaitTime <= 0) {
            throw nonPositiveNanosToWait(maxWaitTime);
        }

        return consumeOrAwaitImpl(tokensToConsume, maxWaitTime);
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
    public BucketConfiguration getConfiguration() {
        return configuration;
    }

}