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

package ru.vbukhtoyarov.concurrency.tokenbucket;

import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public interface WaitingStrategy {
    
    void sleep(long millisToAwait) throws InterruptedException;

    /**
     * This is true waiting strategy, uses {@link java.util.concurrent.locks.LockSupport#parkNanos(long)} inside.
     */
    public static final WaitingStrategy PARKING_WAIT_STRATEGY = new WaitingStrategy() {
        @Override
        public void sleep(long millisToAwait) throws InterruptedException {
            LockSupport.parkNanos(millisToAwait);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    };

    /**
     * Sleeps for the smallest unit of time possible just to relinquish control and to allow other threads to run.
     */
    public static final WaitingStrategy YIELDING_WAIT_STRATEGY = new WaitingStrategy() {
        @Override
        public void sleep(long millisToAwait) throws InterruptedException {
            LockSupport.parkNanos(1);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    };

    /**
     * Does nothing, behaves like spin loop.
     */
    public static final WaitingStrategy SPINLOOP_WAIT_STRATEGY = new WaitingStrategy() {
        @Override
        public void sleep(long millisToAwait) throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    };

}
