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
 * The continuous-state leaky bucket can be viewed as a finite capacity bucket
 * whose real-valued content drains out at a continuous rate of 1 unit of content per time unit
 * and whose content is increased by the increment T for each conforming cell...
 * If at a cell arrival the content of the bucket is less than or equal to the limit value τ, then the cell is conforming;
 * otherwise, the cell is non-conforming.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Token_bucket">Token Bucket</a>
 * @see <a href="http://en.wikipedia.org/wiki/Leaky_bucket">Leaky Bucket</a>
 * @see <a href="http://en.wikipedia.org/wiki/Generic_cell_rate_algorithm">Generic cell rate algorithm</a>
 */
public interface Bucket {

    /**
     * Attempt to consume a single token from the bucket.  If it was consumed then {@code true} is returned, otherwise
     * {@code false} is returned. This is equivalent for {@code tryConsume(1)}
     *
     * @return {@code true} if a token was consumed, {@code false} otherwise.
     */
    boolean tryConsumeSingleToken();

    /**
     * Attempt to consume a specified number of tokens from the bucket.  If the tokens were consumed then {@code true}
     * is returned, otherwise {@code false} is returned.
     *
     * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
     * @return {@code true} if the tokens were consumed, {@code false} otherwise.
     */
    boolean tryConsume(long numTokens);

    /**
     * Consumes as much tokens from bucket as available in the bucket in moment of invocation.
     *
     * @return number of tokens which has been consumed, or zero if was consumed nothing.
     */
    long consumeAsMuchAsPossible();

    /**
     * Consumes as much tokens from bucket as available in the bucket in moment of invocation,
     * but tokens which should be consumed is limited by than not more than {@code limit}.
     *
     * @param limit maximum nubmer of tokens to consume, should be positive.
     *
     * @return number of tokens which has been consumed, or zero if was consumed nothing.
     */
    long consumeAsMuchAsPossible(long limit);

    /**
     * Consumes a single token from the bucket.  If no token is currently available then this method will block until a
     * token becomes available or current thread is interrupted. This is equivalent for {@code consume(1)}
     *
     * @throws InterruptedException in case of current thread has been interrupted during waiting
     */
    void consumeSingleToken() throws InterruptedException;

    /**
     * Consumes a single token from the bucket. If enough tokens are not currently available then this method will block
     * until required number of tokens will be available or current thread is interrupted.
     *
     * @param numTokens The number of tokens to consumeSingleToken from teh bucket, must be a positive number.
     *
     * @throws InterruptedException in case of current thread has been interrupted during waiting
     */
    void consume(long numTokens) throws InterruptedException;

    /**
     * Consumes a single token from the bucket. If no token is currently available then this method will block
     * until  required number of tokens will be available or current thread is interrupted, or {@code maxWaitTime} has elapsed.
     *
     * This is equivalent for {@code tryConsume(1, maxWaitTime)}
     *
     * @param maxWaitTime limit of time which thread can wait.
     *
     * @return true if token has been consumed or false when token has not been consumed
     *
     * @throws InterruptedException in case of current thread has been interrupted during waiting
     */
    boolean tryConsumeSingleToken(long maxWaitTime) throws InterruptedException;

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
    boolean tryConsume(long numTokens, long maxWaitTime) throws InterruptedException;

    BucketState createSnapshot();

    BucketConfiguration getConfiguration();

}
