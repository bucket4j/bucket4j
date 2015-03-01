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
package ru.vbukhtoyarov.concurrency.tokenbucket;

import ru.vbukhtoyarov.concurrency.tokenbucket.refill.FixedIntervalRefillStrategy;
import ru.vbukhtoyarov.concurrency.tokenbucket.refill.RefillStrategy;
import ru.vbukhtoyarov.concurrency.tokenbucket.wrapper.NanoTimeWrapper;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Static utility methods pertaining to creating {@link TokenBucketImpl} instances.
 */
public final class TokenBuckets {
    private TokenBuckets() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long capacity = null;
        private RefillStrategy refillStrategy = null;
        private TokenBucketImpl.SleepStrategy sleepStrategy = YIELDING_SLEEP_STRATEGY;
        private final NanoTimeWrapper ticker = NanoTimeWrapper.SYSTEM;

        public Builder withCapacity(long numTokens) {
            if (numTokens <= 0) {
                throw new IllegalArgumentException("Must specify a positive number of tokens");
            }

            capacity = numTokens;
            return this;
        }

        /**
         * Refill tokens at a fixed interval.
         */
        public Builder withFixedIntervalRefillStrategy(long refillTokens, long period, TimeUnit unit) {
            return withRefillStrategy(new FixedIntervalRefillStrategy(refillTokens, period, unit));
        }

        /**
         * Use a user defined refill strategy.
         */
        public Builder withRefillStrategy(RefillStrategy refillStrategy) {
            this.refillStrategy = Objects.requireNonNull(refillStrategy);
            return this;
        }

        /**
         * Use a sleep strategy that will always attempt to yield the CPU to other processes.
         */
        public Builder withYieldingSleepStrategy() {
            return withSleepStrategy(YIELDING_SLEEP_STRATEGY);
        }

        /**
         * Use a sleep strategy that will not yield the CPU to other processes.  It will busy wait until more tokens become
         * available.
         */
        public Builder withBusyWaitSleepStrategy() {
            return withSleepStrategy(BUSY_WAIT_SLEEP_STRATEGY);
        }

        /**
         * Use a user defined sleep strategy.
         */
        public Builder withSleepStrategy(TokenBucket.SleepStrategy sleepStrategy) {
            this.sleepStrategy = Objects.requireNonNull(sleepStrategy);
            return this;
        }

        /**
         * Build the token bucket.
         */
        public TokenBucketImpl build() {
            Objects.requireNonNull(capacity, "Must specify a capacity");
            Objects.requireNonNull(refillStrategy, "Must specify a refill strategy");

            return new TokenBucketImpl(capacity, capacity, refillStrategy, sleepStrategy, NanoTimeWrapper.SYSTEM);
        }
    }

    private static final TokenBucketImpl.SleepStrategy YIELDING_SLEEP_STRATEGY = new TokenBucketImpl.SleepStrategy() {
        @Override
        public void sleep() {
            // Sleep for the smallest unit of time possible just to relinquish control
            // and to allow other threads to run.
            LockSupport.parkNanos(1);
        }
    };

    private static final TokenBucketImpl.SleepStrategy BUSY_WAIT_SLEEP_STRATEGY = new TokenBucketImpl.SleepStrategy() {
        @Override
        public void sleep() {
            // Do nothing, don't sleep.
        }
    };
}
