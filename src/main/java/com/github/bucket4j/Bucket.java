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
public interface Bucket {
    /**
     * Attempt to consume a single token from the bucket.  If it was consumed then {@code true} is returned, otherwise
     * {@code false} is returned.
     *
     * @return {@code true} if a token was consumed, {@code false} otherwise.
     */
    boolean tryConsumeSingleToken();

    /**
     * Attempt to consume a specified number of tokens from the bucket.  If the tokens were consumed then {@code true}
     * is returned, otherwise {@code false} is returned.
     *
     * @param numTokens The number of tokens to consumeSingleToken from the bucket, must be a positive number.
     * @return {@code true} if the tokens were consumed, {@code false} otherwise.
     */
    boolean tryConsume(long numTokens);

    long consumeAsMuchAsPossible();

    long consumeAsMuchAsPossible(long limit);

    /**
     * Consume a single token from the bucket.  If no token is currently available then this method will block until a
     * token becomes available.
     *
     * @throws InterruptedException
     */
    void consumeSingleToken() throws InterruptedException;

    /**
     * Consumes multiple tokens from the bucket.  If enough tokens are not currently available then this method will block
     * until
     *
     * @param numTokens The number of tokens to consumeSingleToken from teh bucket, must be a positive number.
     *
     * @throws InterruptedException
     */
    void consume(long numTokens) throws InterruptedException;

    boolean tryConsumeSingleToken(long maxWaitNanos) throws InterruptedException;

    boolean tryConsume(long numTokens, long maxWaitNanos) throws InterruptedException;

    BucketState createSnapshot();

    BucketConfiguration getConfiguration();

}
