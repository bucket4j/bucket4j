/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
 * %%
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
 * =========================LICENSE_END==================================
 */
package io.github.bucket4j;

import io.github.bucket4j.local.LocalBucketBuilder;

import java.time.Duration;

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
     * Creates the new builder of in-memory buckets.
     *
     * @return new instance of {@link LocalBucketBuilder}
     */
    static LocalBucketBuilder builder() {
        return new LocalBucketBuilder();
    }

    /**
     * Returns the {@link BlockingBucket} view of this bucket, that provides operations which are able to block caller thread.
     *
     * @return the view to bucket that can be used as scheduler
     */
    BlockingBucket asBlocking();

    /**
     * TODO
     */
    ScheduledBucket asScheduler();

    /**
     * Returns the verbose view of this bucket.
     */
    VerboseBucket asVerbose();

    /**
     * Tries to consume a specified number of tokens from this bucket.
     *
     * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
     *
     * @return {@code true} if the tokens were consumed, {@code false} otherwise.
     */
    boolean tryConsume(long numTokens);

    /**
     * Consumes {@code tokens} from bucket ignoring all limits.
     * In result of this operation amount of tokens in the bucket could became negative.
     *
     * There are two possible reasons to use this method:
     * <ul>
     * <li>An operation with high priority should be executed independently of rate limits, but it should take effect to subsequent operation with bucket.</li>
     * <li>You want to apply custom blocking strategy instead of default which applied on {@code asScheduler().consume(tokens)} </li>
     * </ul>
     *
     * @param tokens amount of tokens that should be consumed from bucket.
     *
     * @return
     * the amount of rate limit violation in nanoseconds calculated in following way:
     * <ul>
     *     <li><tt>zero</tt> if rate limit was not violated. For example bucket had 5 tokens before invocation of {@code consumeIgnoringRateLimits(2)},
     *     after invocation there are 3 tokens remain in the bucket, since limits were not violated <tt>zero</tt> returned as result.</li>
     *     <li>Positive value which describes the amount of rate limit violation in nanoseconds.
     *     For example bucket with limit 10 tokens per 1 second, currently has the 2 tokens available, last refill happen 100 milliseconds ago, and {@code consumeIgnoringRateLimits(6)} called.
     *     <tt>300_000_000</tt> will be returned as result and available tokens in the bucket will became <tt>-3</tt>, and any variation of {@code tryConsume...} will not be successful for 400 milliseconds(time required to refill amount of available tokens until 1).
     *     </li>
     * </ul>
     */
    long consumeIgnoringRateLimits(long tokens);

    /**
     * Tries to consume a specified number of tokens from this bucket.
     *
     * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
     *
     * @return {@link ConsumptionProbe} which describes both result of consumption and tokens remaining in the bucket after consumption.
     */
    ConsumptionProbe tryConsumeAndReturnRemaining(long numTokens);

    /**
     * Estimates ability to consume a specified number of tokens.
     *
     * @param numTokens The number of tokens to consume, must be a positive number.
     *
     * @return {@link EstimationProbe} which describes the ability to consume.
     */
    EstimationProbe estimateAbilityToConsume(long numTokens);

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
     * Add <tt>tokensToAdd</tt> to bucket. In opposite to {@link #addTokens(long)} usage of this method can lead to overflow bucket capacity.
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
    void forceAddTokens(long tokensToAdd);

    /**
     * Returns amount of available tokens in this bucket.

     * <p> This method designed to be used only for monitoring and testing, you should never use this method for business cases,
     * because available tokens can be changed by concurrent transactions for case of multithreaded/multi-process environment.
     *
     * @return amount of available tokens
     */
    long getAvailableTokens();

    /**
     * Replaces configuration of this bucket.
     *
     * <p>
     * The first hard problem of configuration replacement is making decision how to propagate available tokens from bucket with previous configuration to bucket with new configuration.
     * If you don't care about previous bucket state then use {@link TokensInheritanceStrategy#RESET}.
     * But it becomes to a tricky problem when we expect that previous consumption(that has not been compensated by refill yet) should take effect to the bucket with new configuration.
     * In this case you need to make a choice between {@link TokensInheritanceStrategy#PROPORTIONALLY} and {@link TokensInheritanceStrategy#AS_IS}, read documentation about both with strong attention.
     *
     * <p> There is another problem when you are choosing {@link TokensInheritanceStrategy#PROPORTIONALLY} and {@link TokensInheritanceStrategy#AS_IS} and bucket has more then one bandwidth.
     * For example how does replaceConfiguration implementation should bind bandwidths to each other in the following example?
     * <pre>
     * <code>
     *     Bucket bucket = Bucket4j.builder()
     *                       .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
     *                       .addLimit(Bandwidth.simple(10000, Duration.ofHours(1)))
     *                       .build();
     *     ...
     *     BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
     *                                               .addLimit(Bandwidth.simple(5000, Duration.ofHours(1)))
     *                                               .addLimit(Bandwidth.simple(100, Duration.ofSeconds(10)))
     *                                               .build();
     *     bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS);
     * </code>
     * </pre>
     * It is obviously that simple strategy - copying tokens by bandwidth index will not work well in this case, because of it highly depends from order.
     * Instead of inventing the backward maggic Bucket4j provides to you ability to deap controll of this process by specifying identifiers for bandwidth,
     * so in case of multiple bandwidth configuratoin replacement code can copy available tokens by bandwidth ID. So it is better to rewrite code above as following:
     * <pre>
     * <code>
     * Bucket bucket = Bucket4j.builder()
     *                            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)).withId("technical-limit"))
     *                            .addLimit(Bandwidth.simple(10000, Duration.ofHours(1)).withId("business-limit"))
     *                            .build();
     * ...
     * BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
     *                            .addLimit(Bandwidth.simple(5000, Duration.ofHours(1)).withId("business-limit"))
     *                            .addLimit(Bandwidth.simple(100, Duration.ofSeconds(10)).withId("technical-limit"))
     *                            .build();
     * bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS);
     * </code>
     * </pre>
     *
     *
     * <p>
 *     There are following rules for bandwidth identifiers:
     * <ul>
     *     <li>
     *          By default bandwidth has <b>null</b> identifier.
     *     </li>
     *     <li>
     *         null value of identifier equals to another null value if and only if there is only one bandwidth with null identifier.
     *     </li>
     *     <li>
     *         If identifier for bandwidth is specified then it must has unique in the bucket. Bucket does not allow to create several bandwidth with same ID.
     *     </li>
     *     <li>
     *         {@link TokensInheritanceStrategy#RESET} strategy will be applied for tokens migration during config replacement for bandwidth which has no bound bandwidth with same ID in previous configuration,
     *         idependently of strategy that was requested.
     *     </li>
     * </ul>
     *
     * @param newConfiguration the new configuration
     * @param tokensInheritanceStrategy specifies the rules for inheritance of available tokens
     */
     void replaceConfiguration(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy);

    /**
     * Creates the copy of internal state.
     *
     * <p> This method is designed to be used only for monitoring and testing, you should never use this method for business cases.
     *
     * @return snapshot of internal state
     *
     * // TODO remove this method
     */
    BucketState createSnapshot();

    /**
     * Returns new copy of this bucket instance decorated by {@code listener}.
     * The created bucket will share same tokens with source bucket and vice versa.
     *
     * See javadocs for {@link BucketListener} in order to understand semantic of listener.
     *
     * @param listener the listener of bucket events.
     *
     * @return new bucket instance decorated by {@code listener}
     */
    Bucket toListenable(BucketListener listener);

}
