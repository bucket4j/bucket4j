/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.bucket4j;


import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * An abstraction over time measurement.
 *
 * @see com.github.bucket4j.TimeMeter#SYSTEM_NANOTIME
 * @see com.github.bucket4j.TimeMeter#SYSTEM_MILLISECONDS
 */
public interface TimeMeter extends Serializable {

    /**
     * @return current time, which can be a milliseconds, nanoseconds, or something else in case of custom implementation.
     */
    long currentTime();

    /**
     * Sleep required amount of time.
     * @param units time to sleep
     * @throws InterruptedException if current tread is interrupted.
     */
    void sleep(long units) throws InterruptedException;

    long toBandwidthPeriod(TimeUnit timeUnit, long period);

    /**
     * The implementation of {@link TimeMeter} which works arround {@link java.lang.System#nanoTime}
     */
    public static final TimeMeter SYSTEM_NANOTIME = new TimeMeter() {
        @Override
        public long currentTime() {
            return System.nanoTime();
        }

        @Override
        public void sleep(long units) throws InterruptedException {
            LockSupport.parkNanos(units);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }

        @Override
        public long toBandwidthPeriod(TimeUnit timeUnit, long period) {
            return timeUnit.toNanos(period);
        }

        @Override
        public String toString() {
            return "SYSTEM_NANOTIME";
        }
    };

    /**
     * The implementation of {@link TimeMeter} which works around {@link java.lang.System#currentTimeMillis}
     */
    public static final TimeMeter SYSTEM_MILLISECONDS = new TimeMeter() {
        @Override
        public long currentTime() {
            return System.currentTimeMillis();
        }

        @Override
        public void sleep(long units) throws InterruptedException {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(units));
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }

        @Override
        public long toBandwidthPeriod(TimeUnit timeUnit, long period) {
            return timeUnit.toMillis(period);
        }

        @Override
        public String toString() {
            return "SYSTEM_MILLISECONDS";
        }

    };

}
