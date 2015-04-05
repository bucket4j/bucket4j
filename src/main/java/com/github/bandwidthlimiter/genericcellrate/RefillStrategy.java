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
package com.github.bandwidthlimiter.genericcellrate;

/**
 * Encapsulation of a refilling strategy for a token bucket.
 */
public interface RefillStrategy {

    /**
     * Returns the number of tokens to add to the token bucket.
     *
     * @param previousRefillNanoTime
     * @param currentNanoTime
     * @param bandwidth
     *
     * @return The number of tokens to add to the token bucket.
     */
    long refill(Bandwidth bandwidth, long previousRefillNanoTime, long currentNanoTime);
    
    long nanosRequiredToRefill(Bandwidth bandwidth, long numTokens);



    public static RefillStrategy MONOTONE = new RefillStrategy() {

        @Override
        public long refill(Bandwidth bandwidth, long previousRefillNanoTime, long currentNanoTime) {
            long calculatedRefill = (currentNanoTime - previousRefillNanoTime) * bandwidth.getMaxCapacity() / bandwidth.getPeriodInNanos();
            return Math.min(bandwidth.getMaxCapacity(), calculatedRefill);
        }

        @Override
        public long nanosRequiredToRefill(Bandwidth bandwidth, long numTokens) {
            return bandwidth.getPeriodInNanos() * numTokens / bandwidth.getMaxCapacity();
        }

    };

    /**
     * A token bucket refill strategy that will provide N tokens for a token bucket to consumeSingleToken every T units of time.
     * The tokens are refilled in bursts rather than at a fixed tokensGeneratedInOneNanosecond.  This refill strategy will never allow more than
     * N * 2 tokens to be consumed during a window of time T.
     */
    public static RefillStrategy BURST = new RefillStrategy() {

        @Override
        public long refill(Bandwidth bandwidth, long previousRefillNanoTime, long currentNanoTime) {
            if (currentNanoTime - previousRefillNanoTime >= bandwidth.getPeriodInNanos()) {
                return bandwidth.getMaxCapacity();
            }
            return 0;
        }

        @Override
        public long nanosRequiredToRefill(Bandwidth bandwidth, long numTokens) {
            return bandwidth.getPeriodInNanos() * numTokens / bandwidth.getMaxCapacity();
        }

    };

}
