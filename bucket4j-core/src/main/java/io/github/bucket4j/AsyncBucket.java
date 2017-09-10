/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Provides asynchronous API for bucket.
 *
 * A bucket provides asynchronous mode support if and only if particular {@link Extension extension} behind this bucket provides asynchronous mode.
 *
 * <p>
 * A special notes about local(in-memory) buckets: Mostly methods(excepting {@link #tryConsume(long, long, ScheduledExecutorService)})
 * from interface {@link AsyncBucket} are useless for local buckets, because local bucket does not communicate with external back-ends, as result any thread is never blocked, and local bucket.
 * But using asynchronous mode together with {@link #tryConsume(long, long, ScheduledExecutorService)} methods,
 * has a sense even for local bucket, because TODO
 *
 * <p> Which thread does completion of future? TODO
 */
public interface AsyncBucket {

    /**
     * Tries to tryConsume a specified number of tokens from this bucket.
     *
     * @param numTokens The number of tokens to tryConsume from the bucket, must be a positive number.
     * @return {@code true} if the tokens were consumed, {@code false} otherwise.
     */
    CompletableFuture<Boolean> tryConsume(long numTokens);

    /**
     * Tries to tryConsume a specified number of tokens from this bucket.
     *
     * @param numTokens The number of tokens to tryConsume from the bucket, must be a positive number.
     * @return {@link ConsumptionProbe} which describes both result of consumption and tokens remaining in the bucket after consumption.
     */
    CompletableFuture<ConsumptionProbe> tryConsumeAndReturnRemaining(long numTokens);

    /**
     * Tries to tryConsume as much tokens from this bucket as available at the moment of invocation.
     *
     * @return number of tokens which has been consumed, or zero if was consumed nothing.
     */
    CompletableFuture<Long> tryConsumeAsMuchAsPossible();

    /**
     * Tries to tryConsume as much tokens from bucket as available in the bucket at the moment of invocation,
     * but tokens which should be consumed is limited by than not more than {@code limit}.
     *
     * @param limit maximum number of tokens to tryConsume, should be positive.
     *
     * @return number of tokens which has been consumed, or zero if was consumed nothing.
     */
    CompletableFuture<Long> tryConsumeAsMuchAsPossible(long limit);

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
     *      wallet.tryConsume(50); // get 50 cents from wallet
     *      try {
     *          buyCocaCola();
     *      } catch(NoCocaColaException e) {
     *          // return money to wallet
     *          wallet.addTokens(50);
     *      }
     * }</pre>
     *
     * @param tokensToAdd number of tokens to add
     * @throws IllegalArgumentException in case of tokensToAdd less than 1
     */
    CompletableFuture<Void> addTokens(long tokensToAdd);

    /**
     * Consumes a specified number of tokens from the bucket. If required count of tokens is not currently available then this method will block
     * until  required number of tokens will be available or current thread is interrupted, or {@code maxWaitTimeNanos} has elapsed.
     *
     * @param numTokens The number of tokens to tryConsume from the bucket.
     * @param maxWaitNanos limit of time which TODO
     * @param scheduler TODO
     *
     * @return true if {@code numTokens} has been consumed or false when {@code numTokens} has not been consumed
     *
     * @throws InterruptedException in case of current thread has been interrupted during waiting
     * @throws IllegalArgumentException if <tt>numTokens</tt> is greater than capacity of bucket
     */
    CompletableFuture<Boolean> tryConsume(long numTokens, long maxWaitNanos, ScheduledExecutorService scheduler) throws InterruptedException;

    /**
     * Asynchronous variance of {@link Bucket#replaceConfiguration(BucketConfiguration)}, follows the same rules and semantic.
     *
     * @param newConfiguration new configuration
     *
     * @return Future which completed normally when reconfiguration done normally.
     * Future will be completed with {@link IncompatibleConfigurationException} if new configuration is incompatible with previous.
     *
     * @see Bucket#replaceConfiguration(BucketConfiguration)
     */
    CompletableFuture<Void> replaceConfiguration(BucketConfiguration newConfiguration);

}
