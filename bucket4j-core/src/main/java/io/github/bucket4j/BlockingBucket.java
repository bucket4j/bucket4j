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

/**
 * Provides blocking API for bucket that allows to use bucket as scheduler.
 */
public interface BlockingBucket {

    /**
     * Tries to consume a specified number of tokens from the bucket.
     *
     * <p>
     * The algorithm is following:
     * <ul>
     *     <li>If bucket has enough tokens, then tokens consumed and <tt>true</tt> returned immediately.</li>
     *     <li>If bucket has no enough tokens,
     *     and required amount of tokens can not be refilled,
     *     even after waiting of <code>maxWaitTimeNanos</code> nanoseconds,
     *     then consumes nothing and returns <tt>false</tt> immediately.
     *     </li>
     *     <li>
     *         If bucket has no enough tokens,
     *         but deficit can be closed in period of time less then <code>maxWaitTimeNanos</code> nanoseconds,
     *         then tokens consumed(reserved in fair manner) from bucket and current thread blocked for a time required to close deficit,
     *         after unblocking method returns <tt>true</tt>.
     *
     *         <p>
     *         <strong>Note:</strong> If InterruptedException happen when thread was blocked
     *         then tokens will be not returned back to bucket,
     *         but you can use {@link Bucket#addTokens(long)} to returned tokens back.
     *     </li>
     * </ul>
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param maxWaitTimeNanos limit of time(in nanoseconds) which thread can wait.
     * @param blockingStrategy specifies the way to block current thread to amount of time required to refill missed number of tokens in the bucket
     *
     * @return true if {@code numTokens} has been consumed or false when {@code numTokens} has not been consumed
     *
     * @throws InterruptedException in case of current thread has been interrupted during the waiting
     */
    boolean tryConsume(long numTokens, long maxWaitTimeNanos, BlockingStrategy blockingStrategy) throws InterruptedException;

    /**
     * This is just overloaded equivalent of {@link #tryConsume(long, long, BlockingStrategy)}
     *
     * @see #tryConsume(long, long, BlockingStrategy)
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param maxWait limit of time which thread can wait.
     * @param blockingStrategy specifies the way to block current thread to amount of time required to refill missed number of tokens in the bucket
     *
     * @return true if {@code numTokens} has been consumed or false when {@code numTokens} has not been consumed
     *
     * @throws InterruptedException in case of current thread has been interrupted during the waiting
     */
    default boolean tryConsume(long numTokens, Duration maxWait, BlockingStrategy blockingStrategy) throws InterruptedException {
        return tryConsume(numTokens, maxWait.toNanos(), blockingStrategy);
    }

    /**
     * This is just overloaded equivalent of {@link #tryConsume(long, long, BlockingStrategy)}
     *
     * @see #tryConsume(long, long, BlockingStrategy)
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param maxWaitTimeNanos limit of time(in nanoseconds) which thread can wait.
     *
     * @return true if {@code numTokens} has been consumed or false when {@code numTokens} has not been consumed
     *
     * @throws InterruptedException in case of current thread has been interrupted during the waiting
     */
    default boolean tryConsume(long numTokens, long maxWaitTimeNanos) throws InterruptedException {
        return tryConsume(numTokens, maxWaitTimeNanos, BlockingStrategy.PARKING);
    }

    /**
     * This is just overloaded equivalent of {@link #tryConsume(long, long, BlockingStrategy)}
     *
     * @see #tryConsume(long, long, BlockingStrategy)
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param maxWait limit of time which thread can wait.
     *
     * @return true if {@code numTokens} has been consumed or false when {@code numTokens} has not been consumed
     *
     * @throws InterruptedException in case of current thread has been interrupted during the waiting
     */
    default boolean tryConsume(long numTokens, Duration maxWait) throws InterruptedException {
        return tryConsume(numTokens, maxWait.toNanos(), BlockingStrategy.PARKING);
    }

    /**
     * Has same semantic with {@link #tryConsume(long, long, BlockingStrategy)} but ignores interrupts(just restores interruption flag on exit).
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param maxWaitTimeNanos limit of time(in nanoseconds) which thread can wait.
     * @param blockingStrategy specifies the way to block current thread to amount of time required to refill missed number of tokens in the bucket
     *
     * @return true if {@code numTokens} has been consumed or false when {@code numTokens} has not been consumed
     *
     * @see #tryConsume(long, long, BlockingStrategy)
     */
    boolean tryConsumeUninterruptibly(long numTokens, long maxWaitTimeNanos, UninterruptibleBlockingStrategy blockingStrategy);

    /**
     * This is just overloaded equivalent of {@link #tryConsumeUninterruptibly(long, long, UninterruptibleBlockingStrategy)}
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param maxWait limit of time which thread can wait.
     * @param blockingStrategy specifies the way to block current thread to amount of time required to refill missed number of tokens in the bucket
     *
     * @return true if {@code numTokens} has been consumed or false when {@code numTokens} has not been consumed
     *
     * @see #tryConsumeUninterruptibly(long, long, UninterruptibleBlockingStrategy)
     */
    default boolean tryConsumeUninterruptibly(long numTokens, Duration maxWait, UninterruptibleBlockingStrategy blockingStrategy) {
        return tryConsumeUninterruptibly(numTokens, maxWait.toNanos(), blockingStrategy);
    }

    /**
     * This is just overloaded equivalent of {@link #tryConsumeUninterruptibly(long, long, UninterruptibleBlockingStrategy)}
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param maxWaitTimeNanos limit of time(in nanoseconds) which thread can wait.
     *
     * @return true if {@code numTokens} has been consumed or false when {@code numTokens} has not been consumed
     *
     * @see #tryConsumeUninterruptibly(long, long, UninterruptibleBlockingStrategy)
     */
    default boolean tryConsumeUninterruptibly(long numTokens, long maxWaitTimeNanos) {
        return tryConsumeUninterruptibly(numTokens, maxWaitTimeNanos, UninterruptibleBlockingStrategy.PARKING);
    }

    /**
     * This is just overloaded equivalent of {@link #tryConsumeUninterruptibly(long, long, UninterruptibleBlockingStrategy)}
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param maxWait limit of time which thread can wait.
     *
     * @return true if {@code numTokens} has been consumed or false when {@code numTokens} has not been consumed
     *
     * @see #tryConsumeUninterruptibly(long, long, UninterruptibleBlockingStrategy)
     */
    default boolean tryConsumeUninterruptibly(long numTokens, Duration maxWait) {
        return tryConsumeUninterruptibly(numTokens, maxWait.toNanos(), UninterruptibleBlockingStrategy.PARKING);
    }

    /**
     * Consumes a specified number of tokens from the bucket.
     *
     * <p>
     * The algorithm is following:
     * <ul>
     *     <li>If bucket has enough tokens, then tokens consumed and method returns immediately.</li>
     *     <li>
     *         If bucket has no enough tokens, then required amount of tokens will be reserved for future consumption
     *         and current thread will be blocked for a time required to close deficit.
     *     </li>
     *     <li>
     *         <strong>Note:</strong> If InterruptedException happen when thread was blocked
     *         then tokens will be not returned back to bucket,
     *         but you can use {@link Bucket#addTokens(long)} to returned tokens back.
     *     </li>
     * </ul>
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param blockingStrategy specifies the way to block current thread to amount of time required to refill missed number of tokens in the bucket
     *
     *
     * @throws InterruptedException in case of current thread has been interrupted during the waiting
     */
    void consume(long numTokens, BlockingStrategy blockingStrategy) throws InterruptedException;

    /**
     * This is just overloaded equivalent of {@link #consume(long, BlockingStrategy)}
     *
     * @param numTokens The number of tokens to consume from the bucket.
     *
     * @throws InterruptedException in case of current thread has been interrupted during the waiting
     *
     * @see #consume(long, BlockingStrategy)
     */
    default void consume(long numTokens) throws InterruptedException {
        consume(numTokens, BlockingStrategy.PARKING);
    }

    /**
     * Has same semantic with {@link #consume(long, BlockingStrategy)} but ignores interrupts(just restores interruption flag on exit).
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param blockingStrategy specifies the way to block current thread to amount of time required to refill missed number of tokens in the bucket
     *
     * @see #consume(long, BlockingStrategy)
     */
    void consumeUninterruptibly(long numTokens, UninterruptibleBlockingStrategy blockingStrategy);

    /**
     * This is just overloaded equivalent of {@link #consumeUninterruptibly(long, UninterruptibleBlockingStrategy)}
     *
     * @param numTokens The number of tokens to consume from the bucket.
     *
     * @see #consumeUninterruptibly(long, UninterruptibleBlockingStrategy)
     */
    default void consumeUninterruptibly(long numTokens) {
        consumeUninterruptibly(numTokens, UninterruptibleBlockingStrategy.PARKING);
    }

}
