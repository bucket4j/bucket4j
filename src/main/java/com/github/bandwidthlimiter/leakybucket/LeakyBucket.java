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

/**
 * A restriction is used for rate limiting access to a portion of code.
 *
 */
public interface LeakyBucket {
    /**
     * Attempt to consumeSingleToken a single token from the bucket.  If it was consumed then {@code true} is returned, otherwise
     * {@code false} is returned.
     *
     * @return {@code true} if a token was consumed, {@code false} otherwise.
     */
    boolean tryConsumeSingleToken();

    /**
     * Attempt to consumeSingleToken a specified number of tokens from the bucket.  If the tokens were consumed then {@code true}
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

}
