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

package com.github.bandwidthlimiter.leakybucket;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public interface TimeMeter {

    long currentTime();

    void sleep(long units) throws InterruptedException;

    static final TimeMeter SYSTEM_NANOTIME = new TimeMeter() {
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

    };

    static final TimeMeter SYSTEM_MILLISECONDS = new TimeMeter() {
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

    };

}
