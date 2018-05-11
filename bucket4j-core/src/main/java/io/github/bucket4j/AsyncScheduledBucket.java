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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Provides async API for bucket that allows to use bucket as async scheduler.
 */
public interface AsyncScheduledBucket {

    /**
     * Tries to consume the specified number of tokens from the bucket.
     *
     * <p>
     * <strong>The algorithm for all type of buckets is following:</strong>
     * <ul>
     *     <li>Implementation issues asynchronous request to back-end behind the bucket(for local bucket it is just a synchronous call) in way which specific for each particular back-end.</li>
     *     <li>Then uncompleted future returned to the caller.</li>
     *     <li>If back-end provides signal(through callback) that asynchronous request failed, then future completed exceptionally.</li>
     *     <li>When back-end provides signal(through callback) that request is done(for local bucket response got immediately), then following post-processing rules will be applied:
     *          <ul>
     *              <li>
     *                  If tokens were consumed then future immediately completed by <tt>true</tt>.
     *              </li>
     *              <li>
     *                  If tokens were not consumed because were not enough tokens in the bucket and <tt>maxWaitNanos</tt> nanoseconds is not enough time to refill deficit,
     *                  then future immediately completed by <tt>false</tt>.
     *              </li>
     *              <li>
     *                  If tokens were reserved(effectively consumed) then <tt>task</tt> to delayed completion will be scheduled to the <tt>scheduler</tt> via {@link ScheduledExecutorService#schedule(Runnable, long, TimeUnit)},
     *                  when delay equals to time required to refill the deficit of tokens. After scheduler executes task the future completed by <tt>true</tt>.
     *              </li>
     *          </ul>
     *     </li>
     * </ul>
     * It is strongly not recommended to do any heavy work in thread which completes the future,
     * because typically this will be a back-end thread which handles NIO selectors,
     * blocking this thread will take negative performance effect to back-end throughput,
     * so you always should resume control flow in another executor via methods like {@link CompletableFuture#thenApplyAsync(Function, Executor)}.
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param maxWaitNanos limit of time(in nanoseconds) which thread can wait.
     * @param scheduler used to delayed future completion
     */
    CompletableFuture<Boolean> tryConsume(long numTokens, long maxWaitNanos, ScheduledExecutorService scheduler);

    /**
     * This is just overloaded equivalent of {@link #tryConsume(long, long, ScheduledExecutorService)}
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param maxWait limit of time which thread can wait.
     * @param scheduler used to delayed future completion
     *
     * @see #tryConsume(long, long, ScheduledExecutorService)
     */
    default CompletableFuture<Boolean> tryConsume(long numTokens, Duration maxWait, ScheduledExecutorService scheduler) {
        return tryConsume(numTokens, maxWait.toNanos(), scheduler);
    }

    /**
     * Consumes the specified number of tokens from the bucket.
     *
     * <p>
     * <strong>The algorithm for all type of buckets is following:</strong>
     * <ul>
     *     <li>Implementation issues asynchronous request to back-end behind the bucket(for local bucket it is just a synchronous call) in way which specific for each particular back-end.</li>
     *     <li>Then uncompleted future returned to the caller.</li>
     *     <li>If back-end provides signal(through callback) that asynchronous request failed, then future completed exceptionally.</li>
     *     <li>When back-end provides signal(through callback) that request is done(for local bucket response got immediately), then following post-processing rules will be applied:
     *          <ul>
     *              <li>
     *                  If tokens were consumed then future immediately completed.
     *              </li>
     *              <li>
     *                  Else tokens reserved(effectively consumed) and <tt>task</tt> to delayed completion will be scheduled to the <tt>scheduler</tt> via {@link ScheduledExecutorService#schedule(Runnable, long, TimeUnit)},
     *                  when delay equals to time required to refill the deficit of tokens. After scheduler executes task the future completed.
     *              </li>
     *          </ul>
     *     </li>
     * </ul>
     * It is strongly not recommended to do any heavy work in thread which completes the future,
     * because typically this will be a back-end thread which handles NIO selectors,
     * blocking this thread will take negative performance effect to back-end throughput,
     * so you always should resume control flow in another executor via methods like {@link CompletableFuture#thenApplyAsync(Function, Executor)}.
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param scheduler used to delayed future completion
     *
     */
    CompletableFuture<Void> consume(long numTokens, ScheduledExecutorService scheduler);

}
