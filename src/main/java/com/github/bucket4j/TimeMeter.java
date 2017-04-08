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
     * Returns current time in nanosecond precision, but not necessarily nanosecond resolution.
     *
     * @return current time in nanoseconds
     */
    long currentTimeNanos();

    /**
     * Park current thread to required duration of nanoseconds.
     * Throws {@link InterruptedException} in case of current thread was interrupted.
     *
     * @param nanosToPark time to park
     *
     * @throws InterruptedException if current tread is interrupted.
     */
    default void park(long nanosToPark) throws InterruptedException {
        final long endNanos = System.nanoTime() + nanosToPark;
        while (true) {
            LockSupport.parkNanos(nanosToPark);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            long currentTimeNanos = System.nanoTime();
            // it is need to compare deltas instead of direct values in order to solve potential overflow of nano-time
            if (currentTimeNanos - endNanos >= 0) {
                return;
            }
            nanosToPark = endNanos - currentTimeNanos;
        }
    }

    /**
     * Parks current thread to required duration of nanoseconds ignoring all interrupts,
     * if interrupt was happen then interruption flag will be restored on the current thread.
     *
     * @param nanosToPark nanos to park
     */
    default void parkUninterruptibly(long nanosToPark) {
        final long endNanos = System.nanoTime() + nanosToPark;
        boolean interrupted = Thread.interrupted();
        try {
            while (true) {
                LockSupport.parkNanos(nanosToPark);
                if (Thread.interrupted()) {
                    interrupted = true;
                }

                long currentTimeNanos = System.nanoTime();
                // it is need to compare deltas instead of direct values in order to solve potential overflow of nano-time
                if (currentTimeNanos - endNanos >= 0) {
                    return;
                }
                nanosToPark = endNanos - currentTimeNanos;
            }
        } finally {
            if (interrupted) {
                // restore interrupted status
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * The implementation of {@link TimeMeter} which works arround {@link java.lang.System#nanoTime}
     */
    TimeMeter SYSTEM_NANOTIME = new TimeMeter() {
        @Override
        public long currentTimeNanos() {
            return System.nanoTime();
        }

        @Override
        public String toString() {
            return "SYSTEM_NANOTIME";
        }
    };

    /**
     * The implementation of {@link TimeMeter} which works around {@link java.lang.System#currentTimeMillis}
     */
    TimeMeter SYSTEM_MILLISECONDS = new TimeMeter() {
        @Override
        public long currentTimeNanos() {
            long nowMillis = System.currentTimeMillis();
            return TimeUnit.MILLISECONDS.toNanos(nowMillis);
        }

        @Override
        public String toString() {
            return "SYSTEM_MILLISECONDS";
        }
    };

}
