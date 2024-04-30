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

import java.time.Duration;

/**
 * Verbose API for {@link BlockingBucket}
 */
public interface VerboseBlockingBucket {

    /**
     * Has the same semantic as {@link BlockingBucket#tryConsume(long, long, BlockingStrategy)}
     */
    boolean tryConsume(long numTokens, long maxWaitTimeNanos, BlockingStrategy blockingStrategy) throws InterruptedException;

    /**
     * Has the same semantic as {@link BlockingBucket#tryConsume(long, Duration, BlockingStrategy)}
     */
    default boolean tryConsume(long numTokens, Duration maxWait, BlockingStrategy blockingStrategy) throws InterruptedException {
        return tryConsume(numTokens, maxWait.toNanos(), blockingStrategy);
    }

    /**
     * Has the same semantic as {@link BlockingBucket#tryConsume(long, long)}
     */
    default boolean tryConsume(long numTokens, long maxWaitTimeNanos) throws InterruptedException {
        return tryConsume(numTokens, maxWaitTimeNanos, BlockingStrategy.PARKING);
    }

    /**
     * Has the same semantic as {@link BlockingBucket#tryConsume(long, Duration)}
     */
    default boolean tryConsume(long numTokens, Duration maxWait) throws InterruptedException {
        return tryConsume(numTokens, maxWait.toNanos(), BlockingStrategy.PARKING);
    }

    /**
     * Has the same semantic as {@link BlockingBucket#tryConsumeUninterruptibly(long, long, UninterruptibleBlockingStrategy)}
     */
    boolean tryConsumeUninterruptibly(long numTokens, long maxWaitTimeNanos, UninterruptibleBlockingStrategy blockingStrategy);

    /**
     * Has the same semantic as {@link BlockingBucket#tryConsumeUninterruptibly(long, Duration, UninterruptibleBlockingStrategy)}
     */
    default boolean tryConsumeUninterruptibly(long numTokens, Duration maxWait, UninterruptibleBlockingStrategy blockingStrategy) {
        return tryConsumeUninterruptibly(numTokens, maxWait.toNanos(), blockingStrategy);
    }

    /**
     * Has the same semantic as {@link BlockingBucket#tryConsumeUninterruptibly(long, long)}
     */
    default boolean tryConsumeUninterruptibly(long numTokens, long maxWaitTimeNanos) {
        return tryConsumeUninterruptibly(numTokens, maxWaitTimeNanos, UninterruptibleBlockingStrategy.PARKING);
    }

    /**
     * Has the same semantic as {@link BlockingBucket#tryConsumeUninterruptibly(long, Duration)}
     */
    default boolean tryConsumeUninterruptibly(long numTokens, Duration maxWait) {
        return tryConsumeUninterruptibly(numTokens, maxWait.toNanos(), UninterruptibleBlockingStrategy.PARKING);
    }

    /**
     * Has the same semantic as {@link BlockingBucket#consume(long, BlockingStrategy)}
     */
    void consume(long numTokens, BlockingStrategy blockingStrategy) throws InterruptedException;

    /**
     * Has the same semantic as {@link BlockingBucket#consume(long)}
     */
    default void consume(long numTokens) throws InterruptedException {
        consume(numTokens, BlockingStrategy.PARKING);
    }

    /**
     * Has the same semantic as {@link BlockingBucket#consumeUninterruptibly(long, UninterruptibleBlockingStrategy)}
     */
    void consumeUninterruptibly(long numTokens, UninterruptibleBlockingStrategy blockingStrategy);

    /**
     * Has the same semantic as {@link BlockingBucket#consumeUninterruptibly(long)}
     */
    default void consumeUninterruptibly(long numTokens) {
        consumeUninterruptibly(numTokens, UninterruptibleBlockingStrategy.PARKING);
    }

}
