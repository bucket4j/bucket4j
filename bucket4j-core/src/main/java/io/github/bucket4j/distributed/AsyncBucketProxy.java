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
package io.github.bucket4j.distributed;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.proxy.RemoteAsyncBucketBuilder;
import io.github.bucket4j.distributed.proxy.optimization.Optimization;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Asynchronous analog of {@link BucketProxy}.
 */
public interface AsyncBucketProxy {

    /**
     * Returns asynchronous view of this bucket that allows to use bucket as async scheduler.
     *
     * @return Asynchronous view of this bucket that allows to use bucket as async scheduler.
     */
    ScheduledBucket asScheduler();

    /**
     * Returns the verbose view of this bucket.
     */
    AsyncVerboseBucket asVerbose();

    /**
     * Asynchronous version of {@link Bucket#tryConsume(long)}, follows the same semantic.
     *
     * <p>
     * <strong>The algorithm for distribute buckets is following:</strong>
     * <ul>
     *     <li>Implementation issues asynchronous request to back-end behind the bucket in way which specific for each particular back-end.</li>
     *     <li>Then uncompleted future returned to the caller.</li>
     *     <li>When back-end provides signal(through callback) that request is done, then future completed.</li>
     *     <li>If back-end provides signal(through callback) that asynchronous request failed, then future completed exceptionally.</li>
     * </ul>
     * It is strongly not recommended to do any heavy work in thread which completes the future,
     * because typically this will be a back-end thread which handles NIO selectors,
     * blocking this thread will take negative performance effect to back-end throughput,
     * so you always should resume control flow in another executor via methods like {@link CompletableFuture#thenApplyAsync(Function, Executor)}.
     *
     * <p>
     * <strong>The algorithm for local buckets is following:</strong>
     * <ul>
     *     <li>Implementation just redirects request to synchronous version {@link Bucket#tryConsume(long)}</li>
     *     <li>Then returns feature immediately completed by results from previous step. So using this method for local buckets is useless,
     *     because there are no differences with synchronous version.</li>
     * </ul>
     *
     * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
     *
     * @return the future which eventually will be completed by {@code true} if the <tt>numTokens</tt> were consumed and completed by {@code false} otherwise.
     *
     * @see Bucket#tryConsume(long)
     */
    CompletableFuture<Boolean> tryConsume(long numTokens);

    /**
     * Asynchronous version of {@link Bucket#consumeIgnoringRateLimits(long)}, follows the same semantic.
     *
     * @see Bucket#consumeIgnoringRateLimits(long)
     */
    CompletableFuture<Long> consumeIgnoringRateLimits(long tokens);

    /**
     * Asynchronous version of {@link Bucket#tryConsumeAndReturnRemaining(long)}, follows the same semantic.
     *
     * <p>
     * <strong>The algorithm for distribute buckets is following:</strong>
     * <ul>
     *     <li>Implementation issues asynchronous request to back-end behind the bucket in way which specific for each particular back-end.</li>
     *     <li>Then uncompleted future returned to the caller.</li>
     *     <li>When back-end provides signal(through callback) that request is done, then future completed.</li>
     *     <li>If back-end provides signal(through callback) that asynchronous request failed, then future completed exceptionally.</li>
     * </ul>
     * It is strongly not recommended to do any heavy work in thread which completes the future,
     * because typically this will be a back-end thread which handles NIO selectors,
     * blocking this thread will take negative performance effect to back-end throughput,
     * so you always should resume control flow in another executor via methods like {@link CompletableFuture#thenApplyAsync(Function, Executor)}.
     *
     * <p>
     * <strong>The algorithm for local buckets is following:</strong>
     * <ul>
     *     <li>Implementation just redirects request to synchronous version {@link Bucket#tryConsumeAndReturnRemaining(long)}</li>
     *     <li>Then returns feature immediately completed by results from previous step. So using this method for local buckets is useless,
     *     because there are no differences with synchronous version.</li>
     * </ul>
     *
     * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
     *
     * @return the future which eventually will be completed by {@link ConsumptionProbe probe} which describes both result of consumption and tokens remaining in the bucket after consumption.
     *
     * @see Bucket#tryConsumeAndReturnRemaining(long)
     */
    CompletableFuture<ConsumptionProbe> tryConsumeAndReturnRemaining(long numTokens);

    /**
     * Asynchronous version of {@link Bucket#estimateAbilityToConsume(long)}, follows the same semantic.
     *
     * <p>
     * <strong>The algorithm for distribute buckets is following:</strong>
     * <ul>
     *     <li>Implementation issues asynchronous request to back-end behind the bucket in way which specific for each particular back-end.</li>
     *     <li>Then uncompleted future returned to the caller.</li>
     *     <li>When back-end provides signal(through callback) that request is done, then future completed.</li>
     *     <li>If back-end provides signal(through callback) that asynchronous request failed, then future completed exceptionally.</li>
     * </ul>
     * It is strongly not recommended to do any heavy work in thread which completes the future,
     * because typically this will be a back-end thread which handles NIO selectors,
     * blocking this thread will take negative performance effect to back-end throughput,
     * so you always should resume control flow in another executor via methods like {@link CompletableFuture#thenApplyAsync(Function, Executor)}.
     *
     * <p>
     * <strong>The algorithm for local buckets is following:</strong>
     * <ul>
     *     <li>Implementation just redirects request to synchronous version {@link Bucket#estimateAbilityToConsume(long)}</li>
     *     <li>Then returns feature immediately completed by results from previous step. So using this method for local buckets is useless,
     *     because there are no differences with synchronous version.</li>
     * </ul>
     *
     * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
     *
     * @return the future which eventually will be completed by {@link EstimationProbe probe} which describes the ability to consume specified amount of tokens.
     *
     * @see Bucket#estimateAbilityToConsume(long)
     */
    CompletableFuture<EstimationProbe> estimateAbilityToConsume(long numTokens);

    /**
     * Asynchronous version of {@link Bucket#tryConsumeAsMuchAsPossible()}, follows the same semantic.
     *
     * <p>
     * <strong>The algorithm for distribute buckets is following:</strong>
     * <ul>
     *     <li>Implementation issues asynchronous request to back-end behind the bucket in way which specific for each particular back-end.</li>
     *     <li>Then uncompleted future returned to the caller.</li>
     *     <li>When back-end provides signal(through callback) that request is done, then future completed.</li>
     *     <li>If back-end provides signal(through callback) that asynchronous request failed, then future completed exceptionally.</li>
     * </ul>
     * It is strongly not recommended to do any heavy work in thread which completes the future,
     * because typically this will be a back-end thread which handles NIO selectors,
     * blocking this thread will take negative performance effect to back-end throughput,
     * so you always should resume control flow in another executor via methods like {@link CompletableFuture#thenApplyAsync(Function, Executor)}.
     *
     * <p>
     * <strong>The algorithm for local buckets is following:</strong>
     * <ul>
     *     <li>Implementation just redirects request to synchronous version {@link Bucket#tryConsumeAsMuchAsPossible()}</li>
     *     <li>Then returns feature immediately completed by results from previous step. So using this method for local buckets is useless,
     *     because there are no differences with synchronous version.</li>
     * </ul>
     *
     * @return the future which eventually will be completed by number of tokens which has been consumed, or completed by zero if was consumed nothing.
     *
     * @see Bucket#tryConsumeAsMuchAsPossible()
     */
    CompletableFuture<Long> tryConsumeAsMuchAsPossible();

    /**
     * Asynchronous version of {@link Bucket#tryConsumeAsMuchAsPossible(long)}, follows the same semantic.
     *
     * <p>
     * <strong>The algorithm for distribute buckets is following:</strong>
     * <ul>
     *     <li>Implementation issues asynchronous request to back-end behind the bucket in way which specific for each particular back-end.</li>
     *     <li>Then uncompleted future returned to the caller.</li>
     *     <li>When back-end provides signal(through callback) that request is done, then future completed.</li>
     *     <li>If back-end provides signal(through callback) that asynchronous request failed, then future completed exceptionally.</li>
     * </ul>
     * It is strongly not recommended to do any heavy work in thread which completes the future,
     * because typically this will be a back-end thread which handles NIO selectors,
     * blocking this thread will take negative performance effect to back-end throughput,
     * so you always should resume control flow in another executor via methods like {@link CompletableFuture#thenApplyAsync(Function, Executor)}.
     *
     * <p>
     * <strong>The algorithm for local buckets is following:</strong>
     * <ul>
     *     <li>Implementation just redirects request to synchronous version {@link Bucket#tryConsumeAsMuchAsPossible(long)}</li>
     *     <li>Then returns feature immediately completed by results from previous step. So using this method for local buckets is useless,
     *     because there are no differences with synchronous version.</li>
     * </ul>
     *
     * @param limit maximum number of tokens to consume, should be positive.
     *
     * @return the future which eventually will be completed by number of tokens which has been consumed, or completed by zero if was consumed nothing.
     *
     * @see Bucket#tryConsumeAsMuchAsPossible(long)
     */
    CompletableFuture<Long> tryConsumeAsMuchAsPossible(long limit);

    /**
     * Asynchronous version of {@link Bucket#addTokens(long)}, follows the same semantic.
     *
     * <p>
     * <strong>The algorithm for distribute buckets is following:</strong>
     * <ul>
     *     <li>Implementation issues asynchronous request to back-end behind the bucket in way which specific for each particular back-end.</li>
     *     <li>Then uncompleted future returned to the caller.</li>
     *     <li>When back-end provides signal(through callback) that request is done, then future completed.</li>
     *     <li>If back-end provides signal(through callback) that asynchronous request failed, then future completed exceptionally.</li>
     * </ul>
     * It is strongly not recommended to do any heavy work in thread which completes the future,
     * because typically this will be a back-end thread which handles NIO selectors,
     * blocking this thread will take negative performance effect to back-end throughput,
     * so you always should resume control flow in another executor via methods like {@link CompletableFuture#thenApplyAsync(Function, Executor)}.
     *
     * <p>
     * <strong>The algorithm for local buckets is following:</strong>
     * <ul>
     *     <li>Implementation just redirects request to synchronous version {@link Bucket#addTokens(long)}</li>
     *     <li>Then returns feature immediately completed by results from previous step. So using this method for local buckets is useless,
     *     because there are no differences with synchronous version.</li>
     * </ul>
     *
     * @param tokensToAdd number of tokens to add
     *
     * @return the future which eventually will be completed by <tt>null</tt> if operation successfully completed without exception,
     * otherwise(if any exception happen in asynchronous flow) the future will be completed exceptionally.
     *
     * @see Bucket#addTokens(long)
     */
    CompletableFuture<Void> addTokens(long tokensToAdd);

    /**
     * Asynchronous version of {@link Bucket#forceAddTokens(long)}, follows the same semantic.
     *
     * <p>
     * <strong>The algorithm for distribute buckets is following:</strong>
     * <ul>
     *     <li>Implementation issues asynchronous request to back-end behind the bucket in way which specific for each particular back-end.</li>
     *     <li>Then uncompleted future returned to the caller.</li>
     *     <li>When back-end provides signal(through callback) that request is done, then future completed.</li>
     *     <li>If back-end provides signal(through callback) that asynchronous request failed, then future completed exceptionally.</li>
     * </ul>
     * It is strongly not recommended to do any heavy work in thread which completes the future,
     * because typically this will be a back-end thread which handles NIO selectors,
     * blocking this thread will take negative performance effect to back-end throughput,
     * so you always should resume control flow in another executor via methods like {@link CompletableFuture#thenApplyAsync(Function, Executor)}.
     *
     * <p>
     * <strong>The algorithm for local buckets is following:</strong>
     * <ul>
     *     <li>Implementation just redirects request to synchronous version {@link Bucket#addTokens(long)}</li>
     *     <li>Then returns feature immediately completed by results from previous step. So using this method for local buckets is useless,
     *     because there are no differences with synchronous version.</li>
     * </ul>
     *
     * @param tokensToAdd number of tokens to add
     *
     * @return the future which eventually will be completed by <tt>null</tt> if operation successfully completed without exception,
     * otherwise(if any exception happen in asynchronous flow) the future will be completed exceptionally.
     *
     * @see Bucket#addTokens(long)
     */
    CompletableFuture<Void> forceAddTokens(long tokensToAdd);

    /**
     * Has the same semantic with {@link Bucket#replaceConfiguration(BucketConfiguration, TokensInheritanceStrategy)}
     */
    CompletableFuture<Void> replaceConfiguration(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy);

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
    AsyncBucketProxy toListenable(BucketListener listener);

    /**
     * Returns amount of available tokens in this bucket.

     * <p> This method designed to be used only for monitoring and testing, you should never use this method for business cases,
     * because available tokens can be changed by concurrent transactions for case of multithreaded/multi-process environment.
     *
     * @return amount of available tokens
     */
    CompletableFuture<Long> getAvailableTokens();

    /**
     * Returns optimization controller for this proxy.
     *
     * <p>
     * This method is actual only if an optimization was applied during bucket construction via {@link RemoteAsyncBucketBuilder#withOptimization(Optimization)}
     * otherwise returned controller will do nothing.
     *
     * @return optimization controller for this proxy
     */
    AsyncOptimizationController getOptimizationController();

}
