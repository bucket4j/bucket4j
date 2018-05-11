/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j;


import java.util.concurrent.locks.LockSupport;

/**
 * Specifies the way to block current thread to amount of time required to refill missed number of tokens in the bucket.
 *
 * There is default implementation {@link #PARKING},
 * also you can provide any other implementation which for example does something useful instead of blocking(acts as co-routine) or does spin loop.
 */
public interface BlockingStrategy {

    /**
     * Park current thread to required duration of nanoseconds.
     * Throws {@link InterruptedException} in case of current thread was interrupted.
     *
     * @param nanosToPark time to park in nanoseconds
     *
     * @throws InterruptedException if current tread is interrupted.
     */
    void park(long nanosToPark) throws InterruptedException;

    BlockingStrategy PARKING = new BlockingStrategy() {

        @Override
        public void park(final long nanosToPark) throws InterruptedException {
            final long endNanos = System.nanoTime() + nanosToPark;
            long remainingParkNanos = nanosToPark;
            while (true) {
                LockSupport.parkNanos(remainingParkNanos);
                long currentTimeNanos = System.nanoTime();
                remainingParkNanos = endNanos - currentTimeNanos;
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                if (remainingParkNanos <= 0) {
                    return;
                }
            }
        }
    };

}
