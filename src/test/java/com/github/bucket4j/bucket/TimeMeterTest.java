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

package com.github.bucket4j.bucket;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class TimeMeterTest {

    @Test(expected = InterruptedException.class)
    public void sleepForMillisecondTimerShouldThrowExceptionWhenThreadInterrupted() throws InterruptedException {
        Thread.currentThread().interrupt();
        TimeMeter.SYSTEM_MILLISECONDS.sleep(TimeUnit.SECONDS.toMillis(10));
    }

    @Test(expected = InterruptedException.class)
    public void sleepForNanosecondTimerShouldThrowExceptionWhenThreadInterrupted() throws InterruptedException {
        Thread.currentThread().interrupt();
        TimeMeter.SYSTEM_NANOTIME.sleep(TimeUnit.SECONDS.toNanos(10));
    }

}