/*
 * Copyright 2012-2014 Brandon Beck
 *
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
 */
package ru.vbukhtoyarov.concurrency.tokenbucket.refill;

import java.util.concurrent.TimeUnit;

/**
 * A token bucket refill strategy that will provide N tokens for a token bucket to consume every T units of time.
 * The tokens are refilled in bursts rather than at a fixed rate.  This refill strategy will never allow more than
 * N tokens to be consumed during a window of time T.
 */
public class FixedIntervalRefillStrategy implements RefillStrategy {

    private final long numTokens;
    private final long periodInNanos;

    /**
     * Create a FixedIntervalRefillStrategy.
     *
     * @param numTokens The number of tokens to add to the bucket every interval.
     * @param period    How often to refill the bucket.
     * @param unit      Unit for period.
     */
    public FixedIntervalRefillStrategy(long numTokens, long period, TimeUnit unit) {
        this.numTokens = numTokens;
        this.periodInNanos = unit.toNanos(period);
    }

    @Override
    public long refill(long previousRefillNanoTime, long currentNanoTime) {
        if (currentNanoTime - previousRefillNanoTime >= periodInNanos) {
            return numTokens;
        }
        return 0;
    }

    @Override
    public long nanosRequiredToRefill(long numTokens) {
        return periodInNanos * numTokens / this.numTokens;
    }

}

