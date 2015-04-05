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

import com.github.bandwidthlimiter.util.NanoTimeWrapper;

import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.*;

/**
 * A token bucket implementation that is of a leaky bucket in the sense that it has a finite capacity and any added
 * tokens that would exceed this capacity will "overflow" out of the bucket and are lost forever.
 * <p/>
 * In this implementation the rules for refilling the bucket are encapsulated in a provided {@code RefillStrategy}
 * instance.  Prior to attempting to consumeSingleToken any tokens the refill strategy will be consulted to see how many tokens
 * should be added to the bucket.
 * <p/>
 * In addition in this implementation the method of yielding CPU control is encapsulated in the provided
 * {@code SleepStrategy} instance.  For high performance applications where tokens are being refilled incredibly quickly
 * and an accurate bucket implementation is required, it may be useful to never yield control of the CPU and to instead
 * busy wait.  This strategy allows the caller to make this decision for themselves instead of the library forcing a
 * decision.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Token_bucket">Token Bucket on Wikipedia</a>
 * @see <a href="http://en.wikipedia.org/wiki/Leaky_bucket">Leaky Bucket on Wikipedia</a>
 */
public abstract class AbstractLeakyBucket implements LeakyBucket {

    protected static final boolean WAIT_IF_BUSY = true;
    protected static final boolean NO_WAIT_IF_BUSY = false;
    protected static final long UNSPECIFIED_WAITING_LIMIT = -1;

    protected final NanoTimeWrapper nanoTimeWrapper;
    protected final int limitedDimension;
    protected final long smallestCapacity;
    protected final Bandwidth[] limitedBandwidths;
    protected final Bandwidth guaranteedBandwidth;
    protected final boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth;

    protected AbstractLeakyBucket(LeakyBucketConfiguration configuration) {
        this.limitedBandwidths = configuration.getLimitedBandwidths();
        this.guaranteedBandwidth = configuration.getGuaranteedBandwidth();
        this.nanoTimeWrapper = configuration.getNanoTimeWrapper();
        this.limitedDimension = this.limitedBandwidths.length;
        this.smallestCapacity = Bandwidth.getSmallestCapacity(limitedBandwidths);
        this.raiseErrorWhenConsumeGreaterThanSmallestBandwidth = configuration.isRaiseErrorWhenConsumeGreaterThanSmallestBandwidth();
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
            if (raiseErrorWhenConsumeGreaterThanSmallestBandwidth) {
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
            if (raiseErrorWhenConsumeGreaterThanSmallestBandwidth) {
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