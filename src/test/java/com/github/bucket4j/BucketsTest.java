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

import static com.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS;
import static com.github.bucket4j.TimeMeter.SYSTEM_NANOTIME;
import static org.junit.Assert.assertEquals;

public class BucketsTest {

    @Test
    public void testWithNanoTimePrecision() throws Exception {
        assertEquals(SYSTEM_NANOTIME, Buckets.withNanoTimePrecision().getTimeMeter());
    }

    @Test
    public void testWithMillisTimePrecision() throws Exception {
        assertEquals(SYSTEM_MILLISECONDS, Buckets.withMillisTimePrecision().getTimeMeter());
    }

    @Test
    public void testWithCustomTimePrecision() throws Exception {
        assertEquals(SYSTEM_NANOTIME, Buckets.withCustomTimePrecision(SYSTEM_NANOTIME).getTimeMeter());
        assertEquals(SYSTEM_MILLISECONDS, Buckets.withCustomTimePrecision(SYSTEM_MILLISECONDS).getTimeMeter());
    }

    @Test
    public void testToString() throws Exception {
        Buckets.withMillisTimePrecision().withLimitedBandwidth(100, TimeUnit.MILLISECONDS, 10).build().toString();
    }

}