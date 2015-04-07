/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.bandwidthlimiter.leakybucket;

import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.*;

public abstract class AbstractLeakyBucket implements LeakyBucket {

    protected static final long UNSPECIFIED_WAITING_LIMIT = -1;

    protected final long smallestCapacity;
    protected final LeakyBucketConfiguration configuration;

    protected AbstractLeakyBucket(LeakyBucketConfiguration configuration) {
        this.configuration = configuration;
        this.smallestCapacity = Bandwidth.getSmallestCapacity(configuration.getLimitedBandwidths());
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

        if (tokensToConsume > smallestCapacity) {
            if (configuration.isRaiseErrorWhenConsumeGreaterThanSmallestBandwidth()) {
                // illegal api usage detected
                throw tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(tokensToConsume, smallestCapacity);
            } else {
                return false;
            }
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
        if (tokensToConsume > smallestCapacity) {
            // limits will be never satisfied
            throw tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(tokensToConsume, smallestCapacity);
        }
        consumeOrAwaitImpl(tokensToConsume, UNSPECIFIED_WAITING_LIMIT);
    }

    @Override
    public boolean tryConsumeSingleToken(long maxWaitNanos) throws InterruptedException {
        return tryConsume(1, maxWaitNanos);
    }

    @Override
    public boolean tryConsume(long tokensToConsume, long maxWaitNanos) throws InterruptedException {
        if (tokensToConsume <= 0) {
            throw nonPositiveTokensToConsume(tokensToConsume);
        }
        if (tokensToConsume > smallestCapacity) {
            throw tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(tokensToConsume, smallestCapacity);
        }

        if (maxWaitNanos <= 0) {
            throw nonPositiveNanosToWait(maxWaitNanos);
        }

        return consumeOrAwaitImpl(tokensToConsume, maxWaitNanos);
    }

    @Override
    public long consumeAsMuchAsPossible(long limit) {
        if (limit <= 0) {
            throw nonPositiveTokensToConsume(limit);
        }

        if (limit > smallestCapacity) {
            if (configuration.isRaiseErrorWhenConsumeGreaterThanSmallestBandwidth()) {
                // illegal api usage detected
                throw tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(limit, smallestCapacity);
            } else {
                limit = smallestCapacity;
            }
        }
        return consumeAsMuchAsPossibleImpl(limit);
    }

    @Override
    public long consumeAsMuchAsPossible() {
        return consumeAsMuchAsPossibleImpl(smallestCapacity);
    }

}