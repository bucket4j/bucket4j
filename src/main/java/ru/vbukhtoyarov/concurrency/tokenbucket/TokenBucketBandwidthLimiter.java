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
package ru.vbukhtoyarov.concurrency.tokenbucket;

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
public class TokenBucketBandwidthLimiter implements BandwidthLimiter {

    private final NanoTimeWrapper nanoTimeWrapper;
    private final int dimension;
    private final long smallestCapacity;
    private final Bandwidth[] bandwidths;

    private final AtomicReference<ImmutableState> stateReference;

    public TokenBucketBandwidthLimiter(Bandwidth bandwidth, long initialCapacity, NanoTimeWrapper nanoTimeWrapper) {
        this.bandwidths = new Bandwidth[] {bandwidth};
        this.dimension = this.bandwidths.length;
        this.smallestCapacity = bandwidth.getCapacity();
        this.nanoTimeWrapper = nanoTimeWrapper;
        this.stateReference = new AtomicReference(new ImmutableState(new long[] {initialCapacity}, nanoTimeWrapper.nanoTime()));
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
        try {
            return consumeOrAwait(numTokens, false);
        } catch (InterruptedException e) {
            // It should never happen due to waitIfBusy = false
            return ChuckNorris.roundKickExceptionAndGetMeWhatIWant(e);
        }
    }

    /**
     * Consume a single token from the bucket.  If no token is currently available then this method will block until a
     * token becomes available.
     */
    public void consume() throws InterruptedException {
        consume(1);
    }

    /**
     * Consumes multiple tokens from the bucket.  If enough tokens are not currently available then this method will block
     * until
     *
     * @param numTokens The number of tokens to consume from teh bucket, must be a positive number.
     */
    public void consume(long numTokens) throws InterruptedException {
        consumeOrAwait(numTokens, true);
    }

    private boolean consumeOrAwait(long numTokens, boolean waitIfBusy) throws InterruptedException {
        if (numTokens <= 0) {
            throw new IllegalArgumentException("Number of tokens to consume must be positive");
        }
        if (numTokens > smallestCapacity) {
            throw new IllegalArgumentException("Number of tokens to consume must be less than the capacity of the bucket.");
        }

        while (true) {
            long currentNanoTime = nanoTimeWrapper.nanoTime();
            ImmutableState currentState = stateReference.get();

            long currentSizes[] = currentState.size;
            long newSizes[] = new long[currentSizes.length];

            for (int i = 0; i < dimension; i++) {
                Bandwidth bandwidth = bandwidths[i];
                RefillStrategy refillStrategy = bandwidth.getRefillStrategy();
                long refillTokens = refillStrategy.refill(bandwidth, currentState.previuosRefillNanoTime, currentNanoTime);

                long currentSize = currentSizes[i];
                long currentSizeWithRefill = Math.min(bandwidth.getCapacity(), currentSize + refillTokens);
                if (numTokens > currentSizeWithRefill) {
                    if (!waitIfBusy) {
                        return false;
                    }
                    long nanosToWait = refillStrategy.nanosRequiredToRefill(bandwidth, numTokens - currentSizeWithRefill);
                    bandwidth.getWaitingStrategy().sleep(nanosToWait);

                    currentNanoTime = nanoTimeWrapper.nanoTime();
                    currentState = stateReference.get();
                    continue;
                }
                long newSize = currentSizeWithRefill - numTokens;
                newSizes[i] = newSize;
            }

            if (stateReference.compareAndSet(currentState, new ImmutableState(newSizes, currentNanoTime))) {
                return true;
            }
        }
    }

    private static class ImmutableState {

        private final long size[];
        private final long previuosRefillNanoTime;

        private ImmutableState(long size[], long refillNanoTime) {
            this.size = size;
            this.previuosRefillNanoTime = refillNanoTime;
        }
    }

}
