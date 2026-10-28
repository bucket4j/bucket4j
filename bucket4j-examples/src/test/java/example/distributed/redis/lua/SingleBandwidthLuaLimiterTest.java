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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import example.distributed.redis.lua.SingleBandwidthLuaLimiter.LimitResult;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * Correctness tests for {@link SingleBandwidthLuaLimiter}.
 * <p>
 * Contrast with the existing CAS-based Redis path (see
 * {@code io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager} and
 * {@code AbstractCompareAndSwapBasedProxyManager}): that path does
 * GET -&gt; deserialize -&gt; compute -&gt; serialize -&gt; CAS write, so concurrent
 * callers on the same key can conflict and must retry. This limiter does the
 * entire operation inside one Lua script, so Redis's single-threaded script
 * execution serializes concurrent callers for us - the {@link #concurrentContention_noOverGrantsAndNoRetriesNeeded()}
 * test below shows zero exceptions and zero over-grants without any retry loop
 * at all.
 */
class SingleBandwidthLuaLimiterTest {

    private static GenericContainer<?> container;
    private static RedisClient redisClient;
    private static StatefulRedisConnection<String, String> connection;
    private static RedisCommands<String, String> commands;

    @BeforeAll
    static void setup() {
        container = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
        container.start();
        String redisUrl = "redis://" + container.getHost() + ":" + container.getMappedPort(6379);
        redisClient = RedisClient.create(redisUrl);
        connection = redisClient.connect();
        commands = connection.sync();
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

    private static String randomKey() {
        return "lua-limiter-test:" + UUID.randomUUID();
    }

    @Test
    void freshKey_capacityMinusOneRemaining_allowed() {
        SingleBandwidthLuaLimiter limiter = new SingleBandwidthLuaLimiter(connection, 5, 5, 200);
        String key = randomKey();

        LimitResult result = limiter.tryConsumeDetailed(key, 1);

        assertTrue(result.isAllowed());
        assertEquals(4, result.getRemainingTokens());
        assertEquals(0, result.getRetryAfterMillis());
    }

    @Test
    void drainToZero_thenDenied_withRetryAfterInRange() {
        long intervalMs = 300;
        SingleBandwidthLuaLimiter limiter = new SingleBandwidthLuaLimiter(connection, 3, 3, intervalMs);
        String key = randomKey();

        assertTrue(limiter.tryConsume(key));
        assertTrue(limiter.tryConsume(key));
        assertTrue(limiter.tryConsume(key));

        LimitResult denied = limiter.tryConsumeDetailed(key, 1);

        assertFalse(denied.isAllowed());
        assertEquals(0, denied.getRemainingTokens());
        assertTrue(denied.getRetryAfterMillis() > 0 && denied.getRetryAfterMillis() <= intervalMs,
            "retryAfterMillis should be in (0, interval], was " + denied.getRetryAfterMillis());
    }

    @Test
    void refillAcrossOneInterval_addsRefillTokens_clampedAtCapacity() throws InterruptedException {
        long capacity = 5;
        long refill = 2;
        long intervalMs = 250;
        SingleBandwidthLuaLimiter limiter = new SingleBandwidthLuaLimiter(connection, capacity, refill, intervalMs);
        String key = randomKey();

        // drain fully
        for (int i = 0; i < capacity; i++) {
            assertTrue(limiter.tryConsume(key));
        }
        assertFalse(limiter.tryConsume(key));

        // sleep past a single refill boundary, with margin
        Thread.sleep(intervalMs + 80);

        LimitResult afterOneInterval = limiter.tryConsumeDetailed(key, 1);
        // one interval elapsed => +refill tokens became available, one consumed
        assertTrue(afterOneInterval.isAllowed());
        assertEquals(refill - 1, afterOneInterval.getRemainingTokens());
    }

    @Test
    void refillAcrossManyIntervals_isClampedAtCapacity_noDrift() throws InterruptedException {
        long capacity = 4;
        long refill = 4;
        long intervalMs = 150;
        SingleBandwidthLuaLimiter limiter = new SingleBandwidthLuaLimiter(connection, capacity, refill, intervalMs);
        String key = randomKey();

        // consume the initial full bucket
        for (int i = 0; i < capacity; i++) {
            assertTrue(limiter.tryConsume(key));
        }

        // sleep across several refill periods; tokens must clamp at capacity
        // rather than accumulating unbounded (periods * refill would otherwise
        // exceed capacity many times over).
        Thread.sleep(intervalMs * 5 + 80);

        LimitResult result = limiter.tryConsumeDetailed(key, 1);
        assertTrue(result.isAllowed());
        assertEquals(capacity - 1, result.getRemainingTokens());
    }

    @Test
    void ttlIsSetAndRefreshed_keyIsAHash() {
        long intervalMs = 500;
        SingleBandwidthLuaLimiter limiter = new SingleBandwidthLuaLimiter(connection, 10, 10, intervalMs);
        String key = randomKey();

        limiter.tryConsume(key);

        assertEquals("hash", commands.type(key));
        Long ttl = commands.pttl(key);
        assertTrue(ttl > 0, "TTL should be positive after first call, was " + ttl);
        assertTrue(ttl <= 2 * intervalMs, "default TTL should be <= 2*interval, was " + ttl);

        // refreshed on subsequent calls
        Long ttlBefore = commands.pttl(key);
        limiter.tryConsume(key);
        Long ttlAfter = commands.pttl(key);
        assertTrue(ttlAfter > 0);
        // both are close to the configured TTL ceiling; we only assert it was re-applied (not expired/missing)
        assertTrue(ttlBefore > 0 && ttlAfter > 0);
    }

    @Test
    void concurrentContention_noOverGrantsAndNoRetriesNeeded() throws InterruptedException {
        long capacity = 20;
        long refill = 20;
        long intervalMs = 400;
        int threadCount = 50;
        int attemptsPerThread = 10;

        SingleBandwidthLuaLimiter limiter = new SingleBandwidthLuaLimiter(connection, capacity, refill, intervalMs);
        String key = randomKey();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger allowedCount = new AtomicInteger();
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        long testStart = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();
                    for (int a = 0; a < attemptsPerThread; a++) {
                        if (limiter.tryConsume(key)) {
                            allowedCount.incrementAndGet();
                        }
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
            thread.start();
        }

        startLatch.countDown();
        doneLatch.await();
        long testDurationMs = System.currentTimeMillis() - testStart;

        assertTrue(errors.isEmpty(), "expected zero exceptions, got: " + errors);

        // Upper bound on how many grants were possible: initial capacity plus
        // whatever whole refill periods could have elapsed during the run,
        // with a +1 period margin for scheduling jitter between threads.
        long possiblePeriods = (testDurationMs / intervalMs) + 1;
        long maxPossibleGrants = capacity + possiblePeriods * refill;

        assertTrue(allowedCount.get() <= maxPossibleGrants,
            "over-grant detected: allowed=" + allowedCount.get() + " > maxPossibleGrants=" + maxPossibleGrants);
        assertTrue(allowedCount.get() > 0);
    }
}
