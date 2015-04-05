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
package com.github.bandwidthlimiter.genericcellrate.local;

import com.github.bandwidthlimiter.genericcellrate.AbstractTokenBucket;
import com.github.bandwidthlimiter.genericcellrate.ImmutableConfiguration;

public class UnsafeTokenBucket extends AbstractTokenBucket {

    private final TokenBucketState state;

    public UnsafeTokenBucket(ImmutableConfiguration configuration) {
        super(configuration);
        this.state = new TokenBucketState(configuration);
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        long currentNanoTime = nanoTimeWrapper.nanoTime();
        long availableToConsume = state.refill(currentNanoTime, configuration);
        long toConsume = Math.min(limit, availableToConsume);
        state.consume(toConsume);
        return toConsume;
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        long currentNanoTime = nanoTimeWrapper.nanoTime();
        long availableToConsume = state.refill(currentNanoTime, configuration);
        if (tokensToConsume <= availableToConsume) {
            state.consume(tokensToConsume);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyLimitNanos) throws InterruptedException {
        long currentNanoTime = nanoTimeWrapper.nanoTime();
        long availableToConsume = state.refill(currentNanoTime, configuration);

        if (tokensToConsume <= availableToConsume) {
            state.consume(tokensToConsume);
            return true;
        }

        long sleepingLimitNanos = waitIfBusyLimitNanos > 0? waitIfBusyLimitNanos: Long.MAX_VALUE;
        while (true) {
            long deficit = tokensToConsume - availableToConsume;
            if (!state.sleepUntilRefillIfPossible(deficit, sleepingLimitNanos, configuration)) {
                return false;
            }

            currentNanoTime = nanoTimeWrapper.nanoTime();
            availableToConsume = state.refill(currentNanoTime, configuration);
            if (tokensToConsume <= availableToConsume) {
                state.consume(tokensToConsume);
                return true;
            }
        }
    }

}