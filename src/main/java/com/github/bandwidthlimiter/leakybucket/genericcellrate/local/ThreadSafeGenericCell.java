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
package com.github.bandwidthlimiter.leakybucket.genericcellrate.local;


import com.github.bandwidthlimiter.leakybucket.AbstractLeakyBucket;
import com.github.bandwidthlimiter.leakybucket.LeakyBucketConfiguration;
import com.github.bandwidthlimiter.leakybucket.genericcellrate.GenericCellConfiguration;

import java.util.concurrent.atomic.AtomicReference;

public class ThreadSafeGenericCell extends AbstractLeakyBucket {

    private final AtomicReference<GenericCellState> stateReference;
    private final GenericCellConfiguration configuration;

    public ThreadSafeGenericCell(GenericCellConfiguration configuration) {
        super(configuration);
        this.configuration = configuration;
        GenericCellState initialState = new GenericCellState(configuration);
        this.stateReference = new AtomicReference<>(initialState);
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        GenericCellState previousState = stateReference.get();
        GenericCellState newState = new GenericCellState(previousState);
        while (true) {
            long currentNanoTime = nanoTimeWrapper.nanoTime();
            long availableToConsume = newState.refill(currentNanoTime, configuration);
            long toConsume = Math.min(limit, availableToConsume);
            newState.consume(toConsume);
            if (stateReference.compareAndSet(previousState, newState)) {
                return toConsume;
            } else {
                previousState = stateReference.get();
                newState.copyState(previousState);
            }
        }
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        GenericCellState previousState = stateReference.get();
        GenericCellState newState = new GenericCellState(previousState);

        while (true) {
            long currentNanoTime = nanoTimeWrapper.nanoTime();
            long availableToConsume = newState.refill(currentNanoTime, configuration);
            if (tokensToConsume > availableToConsume) {
                return false;
            }
            newState.consume(tokensToConsume);
            if (stateReference.compareAndSet(previousState, newState)) {
                return true;
            } else {
                previousState = stateReference.get();
                newState.copyState(previousState);
            }
        }
    }

    @Override
    protected boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyLimitNanos) throws InterruptedException {
        boolean isWaitingLimited = waitIfBusyLimitNanos > 0;
        final long methodStartNanoTime = isWaitingLimited? nanoTimeWrapper.nanoTime(): 0;
        long currentNanoTime = methodStartNanoTime;
        boolean isFirstCycle = true;
        GenericCellState previousState = stateReference.get();
        GenericCellState newState = new GenericCellState(previousState);

        while (true) {
            if (isFirstCycle) {
                isFirstCycle = false;
            } else {
                currentNanoTime = nanoTimeWrapper.nanoTime();
            }

            long methodDurationNanos = currentNanoTime - methodStartNanoTime;
            if (isWaitingLimited && methodDurationNanos >= waitIfBusyLimitNanos) {
                return false;
            }

            long availableToConsume = newState.refill(currentNanoTime, configuration);
            if (tokensToConsume <= availableToConsume) {
                newState.consume(tokensToConsume);
                if (stateReference.compareAndSet(previousState, newState)) {
                    return true;
                } else {
                    previousState = stateReference.get();
                    newState.copyState(previousState);
                    continue;
                }
            }

            long deficit = tokensToConsume - availableToConsume;
            long sleepingLimitNanos = Long.MAX_VALUE;
            if (isWaitingLimited) {
                sleepingLimitNanos = waitIfBusyLimitNanos - methodDurationNanos;
            }
            if (!newState.sleepUntilRefillIfPossible(deficit, sleepingLimitNanos, configuration)) {
                return false;
            }
        }
    }

}