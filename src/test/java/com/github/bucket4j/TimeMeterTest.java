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

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.assertTrue;

public class TimeMeterTest {


    @Test(expected = InterruptedException.class, timeout = 1000)
    public void sleepForMillisecondTimerShouldThrowExceptionWhenThreadInterrupted() throws InterruptedException {
        Thread.currentThread().interrupt();
        TimeMeter.SYSTEM_MILLISECONDS.park(TimeUnit.SECONDS.toNanos(10));
    }

    @Test(expected = InterruptedException.class, timeout = 1000)
    public void sleepForNanosecondTimerShouldThrowExceptionWhenThreadInterrupted() throws InterruptedException {
        Thread.currentThread().interrupt();
        TimeMeter.SYSTEM_NANOTIME.park(TimeUnit.SECONDS.toNanos(10));
    }

    @Test(timeout = 3000)
    public void sleepForMillisecondTimerShouldHandleSpuriousWakeup() throws InterruptedException {
        // two lines bellow lead to spurious wakeup at first invocation of park
        Thread.currentThread().interrupt();
        Thread.interrupted();

        long startNanos = System.nanoTime();
        long nanosToPark = TimeUnit.SECONDS.toNanos(1);
        TimeMeter.SYSTEM_MILLISECONDS.park(nanosToPark);
        assertTrue(System.nanoTime() - startNanos >= nanosToPark);
    }

    @Test(timeout = 3000)
    public void sleepForNanosecondTimerShouldHandleSpuriousWakeup() throws InterruptedException {
        // two lines bellow lead to spurious wakeup at first invocation of park
        Thread.currentThread().interrupt();
        Thread.interrupted();

        long startNanos = System.nanoTime();
        long nanosToPark = TimeUnit.SECONDS.toNanos(1);
        TimeMeter.SYSTEM_NANOTIME.park(nanosToPark);
        assertTrue(System.nanoTime() - startNanos >= nanosToPark);
    }

    @Test(timeout = 10000)
    public void sleepUniterruptibleShouldNotThrowInterruptedException() {
        long nanosToPark = TimeUnit.SECONDS.toNanos(1);
        Thread.currentThread().interrupt();
        long startNanos = System.nanoTime();
        TimeMeter.SYSTEM_NANOTIME.parkUninterruptibly(nanosToPark);
        assertTrue(System.nanoTime() - startNanos >= nanosToPark);
        assertTrue(Thread.currentThread().isInterrupted());
    }

    public static void main(String[] args) {
        Thread.currentThread().interrupt();
        Thread.interrupted();

        long start = System.nanoTime();
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        System.out.println(System.nanoTime() - start);
    }

}