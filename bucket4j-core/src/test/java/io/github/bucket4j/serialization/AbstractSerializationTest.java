/*
 *
 * Copyright 2015-2019 Vladimir Bukhtoyarov
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

package io.github.bucket4j.serialization;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.EqualityUtils;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static io.github.bucket4j.Bandwidth.classic;
import static io.github.bucket4j.Bandwidth.simple;
import static io.github.bucket4j.Refill.*;
import static java.time.Duration.ofSeconds;
import static org.junit.Assert.assertTrue;

public abstract class AbstractSerializationTest {

    private void testSerialization(Object object) {
        Object object2 = serializeAndDesirialize(object);
        assertTrue(EqualityUtils.equals(object, object2));
    }

    protected abstract <T> T serializeAndDesirialize(T object);


    @Test
    public void serializeSimpleBandwidth() throws IOException {
        Bandwidth bandwidth = simple(10, ofSeconds(20));
        testSerialization(bandwidth);
    }

    @Test
    public void serializeClassicBandwidthWithGreedyRefill() throws IOException {
        Bandwidth bandwidth = classic(20, greedy(100, Duration.ofSeconds(42)));
        testSerialization(bandwidth);
    }

    @Test
    public void serializeClassicBandwidthWithIntervallyRefill() throws IOException {
        Bandwidth bandwidth = classic(30, intervally(200, Duration.ofSeconds(420)));
        testSerialization(bandwidth);
    }

    @Test
    public void serializeClassicBandwidthWithIntervallyAlignedRefill() throws IOException {
        Bandwidth bandwidth = classic(40, intervallyAligned(300, Duration.ofSeconds(4200), Instant.now(), true));
        testSerialization(bandwidth);
    }

}
