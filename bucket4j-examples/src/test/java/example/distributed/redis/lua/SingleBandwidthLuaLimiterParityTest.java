package example.distributed.redis.lua;

/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2026 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * Drives an identical, deterministic timeline of calls through (i) an in-JVM
 * bucket4j {@link Bucket} configured with a single classic bandwidth and
 * fixed-interval ({@code Refill.intervally}) refill, and (ii) the
 * {@link SingleBandwidthLuaLimiter}, asserting they produce the same
 * allow/deny sequence.
 * <p>
 * This is the regression net referenced in the contribution plan (section
 * 9.A): it demonstrates the restricted server-side fast path computes the
 * *same* answers as the general in-JVM path for the configurations it
 * supports (single bandwidth, integer, fixed-interval refill).
 */
class SingleBandwidthLuaLimiterParityTest {

    private static GenericContainer<?> container;
    private static RedisClient redisClient;
    private static StatefulRedisConnection<String, String> connection;

    @BeforeAll
    static void setup() {
        container = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
        container.start();
        String redisUrl = "redis://" + container.getHost() + ":" + container.getMappedPort(6379);
        redisClient = RedisClient.create(redisUrl);
        connection = redisClient.connect();
    }

    @AfterAll
    static void shutdown() {
        try {
            if (connection != null) {
                connection.close();
            }
            if (redisClient != null) {
                redisClient.shutdown();
            }
        } finally {
            if (container != null) {
                container.close();
            }
        }
    }

    @Test
    void identicalAllowDenySequence_forDeterministicTimeline() throws InterruptedException {
        long capacity = 3;
        long refill = 3;
        long intervalMs = 150;
        // Generous margin over the interval so that we are never asserting near
        // a refill boundary (which would make the comparison flaky against two
        // independently-measured clocks - the JVM's and Redis's TIME).
        long marginMs = 60;

        Bucket bucket = Bucket.builder()
            .addLimit(Bandwidth.classic(capacity, Refill.intervally(refill, Duration.ofMillis(intervalMs))))
            .build();

        SingleBandwidthLuaLimiter limiter = new SingleBandwidthLuaLimiter(connection, capacity, refill, intervalMs);
        String key = "lua-limiter-parity:" + UUID.randomUUID();

        // sleepBeforeMs=0 means "call immediately, no sleep before this step".
        // capacity == refill == 3, so every full interval fully replenishes the
        // bucket: 3 immediate allows are always followed by exactly one deny.
        long[] sleepsBeforeEachCallMs = {
            0, 0, 0,                // drain the full initial capacity: allow, allow, allow
            0,                       // bucket empty now: deny
            intervalMs + marginMs, 0, 0, // wait past one refill boundary, then drain again: allow, allow, allow
            0,                       // empty again: deny
            intervalMs + marginMs, 0, 0, // wait past another refill boundary, then drain again: allow, allow, allow
            0                        // empty again: deny
        };

        List<Boolean> bucketResults = new ArrayList<>();
        List<Boolean> limiterResults = new ArrayList<>();

        for (long sleepMs : sleepsBeforeEachCallMs) {
            if (sleepMs > 0) {
                Thread.sleep(sleepMs);
            }
            bucketResults.add(bucket.tryConsume(1));
            limiterResults.add(limiter.tryConsume(key));
        }

        assertEquals(bucketResults, limiterResults,
            "expected identical allow/deny sequence between in-JVM Bucket and Lua limiter");
    }
}
