/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j;

/**
 * Performs rate limiting using algorithm based on top of ideas of <a href="https://en.wikipedia.org/wiki/Token_bucket">Token Bucket</a>.
 * <p>
 * Use following links for further details:
 * <ul>
 * <li><a href="https://github.com/vladimir-bukhtoyarov/bucket4j/blob/4.0/doc-pages/basic-usage.md">Basic example of usage</a></li>
 * <li><a href="https://github.com/vladimir-bukhtoyarov/bucket4j/blob/4.0/doc-pages/advanced-usage.md">Advanced examples of usage</a></li>
 * </ul>
 */
public interface Bucket {

    /**
     * Returns the {@link BlockingBucket} view of this bucket, that provides operations which are able to block caller thread.
     *
     * @return the view to bucket that can be used as scheduler
     */
    BlockingBucket asScheduler();

    /**
     * Describes whether or not this bucket supports asynchronous mode.
     *
     * <p>If asynchronous mode is  not supported any attempt to call {@link #asAsync()} will fail with {@link UnsupportedOperationException}
     *
     * @return true if this extension supports asynchronous mode.
     */
    boolean isAsyncModeSupported();

    /**
     * Returns asynchronous view of this bucket.
     *
     * <p>If asynchronous mode is not supported by particular extension behind this bucket,
     * then any attempt to call this method will fail with {@link UnsupportedOperationException}.
     *
     * @return Asynchronous view of this bucket.
     *
     * @throws UnsupportedOperationException if particular extension behind the bucket does not support asynchronous mode.
     */
    AsyncBucket asAsync();

    /**
     * Returns asynchronous view of this bucket that allows to use bucket as async scheduler.
     *
     * <p>If asynchronous mode is not supported by particular extension behind this bucket,
     * then any attempt to call this method will fail with {@link UnsupportedOperationException}.
     *
     * @return Asynchronous view of this bucket that allows to use bucket as async scheduler.
     *
     * @throws UnsupportedOperationException if particular extension behind the bucket does not support asynchronous mode.
     */
    AsyncScheduledBucket asAsyncScheduler();

    /**
     * Tries to consume a specified number of tokens from this bucket.
     *
     * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
     *
     * @return {@code true} if the tokens were consumed, {@code false} otherwise.
     */
    boolean tryConsume(long numTokens);

    /**
     * Tries to consume a specified number of tokens from this bucket.
     *
     * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
     *
     * @return {@link ConsumptionProbe} which describes both result of consumption and tokens remaining in the bucket after consumption.
     */
    ConsumptionProbe tryConsumeAndReturnRemaining(long numTokens);

    /**
     * Tries to consume as much tokens from this bucket as available at the moment of invocation.
     *
     * @return number of tokens which has been consumed, or zero if was consumed nothing.
     */
    long tryConsumeAsMuchAsPossible();

    /**
     * Tries to consume as much tokens from bucket as available in the bucket at the moment of invocation,
     * but tokens which should be consumed is limited by {@code limit}.
     *
     * @param limit maximum number of tokens to consume, should be positive.
     *
     * @return number of tokens which has been consumed, or zero if was consumed nothing.
     */
    long tryConsumeAsMuchAsPossible(long limit);

    /**
     * Add <tt>tokensToAdd</tt> to bucket.
     * Resulted count of tokens are calculated by following formula:
     * <pre>newTokens = Math.min(capacity, currentTokens + tokensToAdd)</pre>
     * in other words resulted number of tokens never exceeds capacity independent of <tt>tokensToAdd</tt>.
     *
     * <h3>Example of usage</h3>
     * The "compensating transaction" is one of obvious use case, when any piece of code consumed tokens from bucket, tried to do something and failed, the "addTokens" will be helpful to return tokens back to bucket:
     * <pre>{@code
     *      Bucket wallet;
     *      ...
     *      if(wallet.tryConsume(50)) {// get 50 cents from wallet
     *         try {
     *             buyCocaCola();
     *         } catch(NoCocaColaException e) {
     *             // return money to wallet
     *             wallet.addTokens(50);
     *         }
     *      };
     * }</pre>
     *
     * @param tokensToAdd number of tokens to add
     */
    void addTokens(long tokensToAdd);

    /**
     * Returns amount of available tokens in this bucket.

     * <p> This method designed to be used only for monitoring and testing, you should never use this method for business cases,
     * because available tokens can be changed by concurrent transactions for case of multithreaded/multi-process environment.
     *
     * @return amount of available tokens
     */
    long getAvailableTokens();

    /**
     * Replaces configuration of this bucket instance.
     *
     * <p>Rules of reconfiguration:
     * <ul>
     *    <li>
     *      New configuration can be applied if and only if it describes same count of bandwidths which already configured,
     *      else {@link IncompatibleConfigurationException} will be thrown.
     *      In other words you must not try to reduce or increase count of bandwidths,
     *      it is impossible to create bucket with one bandwidth and reconfigure to use two bandwidths and vice-versa.
     *    </li>
     *    <li>
     *        If new configuration defines capacity which greater than current available tokens,
     *        then current available tokens stay unchanged.
     *    </li>
     *    <li>
     *        If new configuration defines capacity which lesser than current available tokens,
     *        then current available tokens will be reduced to capacity.
     *        For example: if bandwidth at moment of reconfiguration has 90 tokens,
     *        and new capacity is 70 tokens then available tokens will be reduced from 90 to 70 according to new rules.
     *    </li>
     * </ul>
     *
     * @param newConfiguration the new configuration for this bucket
     *
     * @throws IncompatibleConfigurationException if {@code newConfiguration} incompatible with previous configuration
     *
     */
     void replaceConfiguration(BucketConfiguration newConfiguration);

    /**
     * Creates the copy of internal state.
     *
     * <p> This method is designed to be used only for monitoring and testing, you should never use this method for business cases.
     *
     * @return snapshot of internal state
     */
    BucketState createSnapshot();

    /**
     * Returns new copy of this bucket instance decorated by {@code listener}.
     *
     * See javadocs for {@link BucketListener} in order to understand semantic of listener.
     *
     * @param listener the listener of bucket events.
     *
     * @return new bucket instance decorated by {@code listener}
     */
    Bucket toListenable(BucketListener listener);

}
