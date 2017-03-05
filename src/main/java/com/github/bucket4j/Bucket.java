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
 * Performs rate limiting using algorithm based on top of ideas of <a href="http://en.wikipedia.org/wiki/Token_bucket">Token Bucket</a>.
 *
 * <h3><a name="blocking-semantic">Classification of consumption methods:</a></h3>
 * The methods for consumption can be classified in two group:
 * <ul>
 *     <li>All methods which name started from "try" return result immediately,
 *     for example {@link #tryConsumeSingleToken()}, {@link #tryConsumeAsMuchAsPossible()}.</li>
 *
 *     <li>All methods which name started from "consume" can block current thread
 *     and wait until requested amount of tokens will be added,
 *     for example {@link #consumeSingleToken()}, {@link #consume(long)}.</li>
 * </ul>
 *
 */
public interface Bucket {

    /**
     * Tries to consume one token from this bucket.
     *
     * <p>
     * This is equivalent for {@code tryConsume(1)}
     *
     * @return {@code true} if a token has been consumed, {@code false} otherwise.
     */
    boolean tryConsumeSingleToken();

    /**
     * Tries to consume a specified number of tokens from this bucket.
     *
     * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
     * @return {@code true} if the tokens were consumed, {@code false} otherwise.
     */
    boolean tryConsume(long numTokens);

    /**
     * Tries to consume as much tokens from this bucket as available at the moment of invocation.
     *
     * @return number of tokens which has been consumed, or zero if was consumed nothing.
     */
    long tryConsumeAsMuchAsPossible();

    /**
     * Tries to consume as much tokens from bucket as available in the bucket at the moment of invocation,
     * but tokens which should be consumed is limited by than not more than {@code limit}.
     *
     * @param limit maximum number of tokens to consume, should be positive.
     *
     * @return number of tokens which has been consumed, or zero if was consumed nothing.
     */
    long tryConsumeAsMuchAsPossible(long limit);

    /**
     * Consumes a single token from the bucket. If no token is currently available then this method will block
     * until  required number of tokens will be available or current thread is interrupted, or {@code maxWaitTime} has elapsed.
     *
     * <p>
     * This is equivalent for {@code consume(1, maxWaitTime)}
     *
     * @param maxWaitTime limit of time which thread can wait.
     *
     * @return true if token has been consumed or false when token has not been consumed
     *
     * @throws InterruptedException in case of current thread has been interrupted during waiting
     */
    boolean consumeSingleToken(long maxWaitTime) throws InterruptedException;

    /**
     * Consumes a specified number of tokens from the bucket. If required count of tokens is not currently available then this method will block
     * until  required number of tokens will be available or current thread is interrupted, or {@code maxWaitTime} has elapsed.
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param maxWaitTime limit of time which thread can wait.
     *
     * @return true if {@code numTokens} has been consumed or false when {@code numTokens} has not been consumed
     *
     * @throws InterruptedException in case of current thread has been interrupted during waiting
     */
    boolean consume(long numTokens, long maxWaitTime) throws InterruptedException;

    /**
     * Consumes a single token from the bucket.  If no token is currently available then this method will block until a
     * token becomes available or current thread is interrupted. This is equivalent for {@code consume(1)}
     *
     * @throws InterruptedException in case of current thread has been interrupted during waiting
     */
    void consumeSingleToken() throws InterruptedException;

    /**
     * Consumes {@code numTokens} from the bucket. If enough tokens are not currently available then this method will block
     * until required number of tokens will be available or current thread is interrupted.
     *
     * @param numTokens The number of tokens to consumeSingleToken from teh bucket, must be a positive number.
     *
     * @throws InterruptedException in case of current thread has been interrupted during waiting
     */
    void consume(long numTokens) throws InterruptedException;

    BucketState createSnapshot();

    BucketConfiguration getConfiguration();

}
