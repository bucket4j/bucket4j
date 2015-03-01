/*
 * Copyright 2012-2014 Brandon Beck
 *
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
 */
package ru.vbukhtoyarov.concurrency.tokenbucket;

import ru.vbukhtoyarov.concurrency.tokenbucket.refill.RefillStrategy;
import ru.vbukhtoyarov.concurrency.tokenbucket.wrapper.NanoTimeWrapper;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A token bucket implementation that is of a leaky bucket in the sense that it has a finite capacity and any added
 * tokens that would exceed this capacity will "overflow" out of the bucket and are lost forever.
 * <p/>
 * In this implementation the rules for refilling the bucket are encapsulated in a provided {@code RefillStrategy}
 * instance.  Prior to attempting to consume any tokens the refill strategy will be consulted to see how many tokens
 * should be added to the bucket.
 * <p/>
 * In addition in this implementation the method of yielding CPU control is encapsulated in the provided
 * {@code SleepStrategy} instance.  For high performance applications where tokens are being refilled incredibly quickly
 * and an accurate bucket implementation is required, it may be useful to never yield control of the CPU and to instead
 * busy wait.  This strategy allows the caller to make this decision for themselves instead of the library forcing a
 * decision.
 */
class TokenBucketImpl implements TokenBucket {
    private final long maxCapacity;
    private final RefillStrategy refillStrategy;
    private final SleepStrategy sleepStrategy;
    private final NanoTimeWrapper nanoTimeWrapper;

    private final AtomicReference<ImmutableState> stateReference;

    TokenBucketImpl(long maxCapacity, long initialCapacity, RefillStrategy refillStrategy, SleepStrategy sleepStrategy, NanoTimeWrapper nanoTimeWrapper) {
        this.maxCapacity = maxCapacity;
        this.refillStrategy = refillStrategy;
        this.sleepStrategy = sleepStrategy;
        this.nanoTimeWrapper = nanoTimeWrapper;
        this.stateReference = new AtomicReference(new ImmutableState(initialCapacity, nanoTimeWrapper.nanoTime()));
    }

    /**
     * Attempt to consume a single token from the bucket.  If it was consumed then {@code true} is returned, otherwise
     * {@code false} is returned.
     *
     * @return {@code true} if a token was consumed, {@code false} otherwise.
     */
    public boolean tryConsume() {
        return tryConsume(1);
    }

    /**
     * Attempt to consume a specified number of tokens from the bucket.  If the tokens were consumed then {@code true}
     * is returned, otherwise {@code false} is returned.
     *
     * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
     * @return {@code true} if the tokens were consumed, {@code false} otherwise.
     */
    public boolean tryConsume(long numTokens) {
        if (numTokens <= 0) {
            throw new IllegalArgumentException("Number of tokens to consume must be positive");
        }
        if (numTokens > maxCapacity) {
            throw new IllegalArgumentException("Number of tokens to consume must be less than the capacity of the bucket.");
        }

        while (true) {
            long currentNanoTime = nanoTimeWrapper.nanoTime();
            ImmutableState currentState = stateReference.get();
            long currentSize = currentState.size;
            long refillTokens = refillStrategy.refill(currentState.previuosRefillNanoTime, currentNanoTime);
            long currentSizeWithRefill = Math.min(maxCapacity, currentSize + refillTokens);
            if (numTokens > currentSizeWithRefill) {
                return false;
            }

            long newSize = currentSizeWithRefill - numTokens;
            if (stateReference.compareAndSet(currentState, new ImmutableState(newSize, currentNanoTime))) {
                return true;
            }
        }
    }

    /**
     * Consume a single token from the bucket.  If no token is currently available then this method will block until a
     * token becomes available.
     */
    public void consume() {
        consume(1);
    }

    /**
     * Consumes multiple tokens from the bucket.  If enough tokens are not currently available then this method will block
     * until
     *
     * @param numTokens The number of tokens to consume from teh bucket, must be a positive number.
     */
    public void consume(long numTokens) {
        while (true) {
            if (tryConsume(numTokens)) {
                return;
            }

            sleepStrategy.sleep();
        }
    }

    private static class ImmutableState {

        private final long size;
        private final long previuosRefillNanoTime;

        private ImmutableState(long size, long refillNanoTime) {
            this.size = size;
            this.previuosRefillNanoTime = refillNanoTime;
        }
    }

}
