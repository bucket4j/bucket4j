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

package com.github.bandwidthlimiter.tokenbucket;

import java.util.concurrent.locks.LockSupport;

public interface WaitingStrategy {
    
    void sleep(long nanosToAwait) throws InterruptedException;

    /**
     * This is fair waiting strategy, uses {@link java.util.concurrent.locks.LockSupport#parkNanos(long)}.
     * This strategy is used by default.
     */
    public static final WaitingStrategy PARKING = new WaitingStrategy() {
        @Override
        public void sleep(long nanosToAwait) throws InterruptedException {
            LockSupport.parkNanos(nanosToAwait);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    };

    /**
     * Sleeps for the smallest unit of time possible just to relinquish control and to allow other threads to run.
     */
    public static final WaitingStrategy YIELDING = new WaitingStrategy() {
        @Override
        public void sleep(long nanosToAwait) throws InterruptedException {
            LockSupport.parkNanos(1);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    };

    /**
     * Does nothing, behaves like spin loop.
     */
    public static final WaitingStrategy SPINLOOP = new WaitingStrategy() {
        @Override
        public void sleep(long nanosToAwait) throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    };

}
