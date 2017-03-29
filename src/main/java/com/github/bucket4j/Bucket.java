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
 * Performs rate limiting using algorithm based on top of ideas of <a href="https://github.com/vladimir-bukhtoyarov/bucket4j/blob/1.3/doc-pages/token-bucket-brief-overview.md">Token Bucket</a>.
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
     * until  required number of tokens will be available or current thread is interrupted, or {@code maxWaitTimeNanos} has elapsed.
     *
     * <p>
     * This is equivalent for {@code consume(1, maxWaitTimeNanos)}
     *
     * @param maxWaitTimeNanos limit of time which thread can wait.
     *
     * @return true if token has been consumed or false when token has not been consumed
     *
     * @throws InterruptedException in case of current thread has been interrupted during waiting
     */
    boolean consumeSingleToken(long maxWaitTimeNanos) throws InterruptedException;

    /**
     * Consumes a specified number of tokens from the bucket. If required count of tokens is not currently available then this method will block
     * until  required number of tokens will be available or current thread is interrupted, or {@code maxWaitTimeNanos} has elapsed.
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param maxWaitTimeNanos limit of time which thread can wait.
     *
     * @return true if {@code numTokens} has been consumed or false when {@code numTokens} has not been consumed
     *
     * @throws InterruptedException in case of current thread has been interrupted during waiting
     * @throws IllegalArgumentException if <tt>numTokens</tt> is greater than capacity of bucket
     */
    boolean consume(long numTokens, long maxWaitTimeNanos) throws InterruptedException;

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
     * @throws IllegalArgumentException if <tt>numTokens</tt> is greater than capacity of bucket
     */
    void consume(long numTokens) throws InterruptedException;

    /**
     * Add <tt>tokensToAdd</tt> to each bandwidth of bucket.
     * Resulted count of tokens are calculated by following formula:
     * <pre>newTokens = Math.min(capacity, currentTokens + tokensToAdd)</pre>
     * in other words resulted number of tokens never exceeds capacity independent of <tt>tokensToAdd</tt>.
     *
     * <h3>Example of usage</h3>
     * The "compensating transaction" is one of obvious use case, when any piece of code consumed tokens from bucket, tried to do something and failed, the "addTokens" will be helpful to return tokens back to bucket:
     * <pre>{@code
     *      Bucket wallet;
     *      ...
     *      wallet.consume(50); // get 50 cents from wallet
     *      try {
     *          buyCocaCola();
     *      } catch(NoCocaColaException e) {
     *          // return money to wallet
     *          wallet.addTokens(50);
     *      }
     * }</pre>
     *
     * @param tokensToAdd number of tokens to add
     * @throws IllegalArgumentException in case of tokensToAdd <= 0
     */
    void addTokens(long tokensToAdd);

    /**
     * Creates the copy of internal state.
     *
     * <p> This method is designed to be used only for monitoring and testing, you should never use this method for business cases.
     *
     * @return snapshot of internal state
     */
    BucketState createSnapshot();

    /**
     * Returns configuration of this bucket.
     *
     * @return configuration
     */
    BucketConfiguration getConfiguration();

}
