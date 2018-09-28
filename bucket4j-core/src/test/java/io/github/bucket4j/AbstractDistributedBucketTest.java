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

import io.github.bucket4j.remote.*;
import io.github.bucket4j.util.ConsumptionScenario;
import org.junit.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.github.bucket4j.remote.RecoveryStrategy.RECONSTRUCT;
import static io.github.bucket4j.remote.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION;
import static org.junit.Assert.*;

public abstract class AbstractDistributedBucketTest {

    private final String key = UUID.randomUUID().toString();
    private final String anotherKey = UUID.randomUUID().toString();
    private final Backend<String> backend = getBackend();

    private RemoteBucketBuilder<String> builderForLongRunningTests = Bucket4j.builder(backend)
            .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)).withInitialTokens(0))
            .addLimit(Bandwidth.simple(200, Duration.ofSeconds(10)).withInitialTokens(0));
    private double permittedRatePerSecond = Math.min(1_000d / 60, 200.0 / 10);

    protected abstract Backend<String> getBackend();

    protected abstract void removeBucketFromBackingStorage(String key);

    @Test
    public void testReconstructRecoveryStrategy() {
        Bucket bucket = Bucket4j.builder(backend)
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(200, Duration.ofSeconds(10)))
                .build(key, RECONSTRUCT);

        assertTrue(bucket.tryConsume(1));

        // simulate crash
        removeBucketFromBackingStorage(key);

        assertTrue(bucket.tryConsume(1));
    }

    @Test
    public void testThrowExceptionRecoveryStrategy() {
        Bucket bucket = Bucket4j.builder(backend)
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build(key, THROW_BUCKET_NOT_FOUND_EXCEPTION);

        assertTrue(bucket.tryConsume(1));

        // simulate crash
        removeBucketFromBackingStorage(key);

        try {
            bucket.tryConsume(1);
            fail();
        } catch (BucketNotFoundException e) {
            // ok
        }
    }

    @Test
    public void testLocateConfigurationThroughProxyManager() {
        ProxyManager<String> proxyManager = Bucket4j.proxyManager(backend);

        // should return empty optional if bucket is not stored
        Optional<BucketConfiguration> remoteConfiguration = proxyManager.getProxyConfiguration(key);
        assertFalse(remoteConfiguration.isPresent());

        // should return not empty options if bucket is stored
        Bucket4j.builder(backend)
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build(key, THROW_BUCKET_NOT_FOUND_EXCEPTION);
        remoteConfiguration = proxyManager.getProxyConfiguration(key);
        assertTrue(remoteConfiguration.isPresent());

        // should return empty optional if bucket is removed
        removeBucketFromBackingStorage(key);
        remoteConfiguration = proxyManager.getProxyConfiguration(key);
        assertFalse(remoteConfiguration.isPresent());
    }

    @Test
    public void testLocateBucketThroughProxyManager() {
        ProxyManager<String> proxyManager = Bucket4j.proxyManager(backend);

        // should return empty optional if bucket is not stored
        Optional<Bucket> remoteBucket = proxyManager.getProxy(key);
        assertFalse(remoteBucket.isPresent());

        // should return not empty options if bucket is stored
        RemoteBucketBuilder<String> builder = Bucket4j.builder(backend)
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(200, Duration.ofSeconds(10)));
        builder.build(key, THROW_BUCKET_NOT_FOUND_EXCEPTION);
        remoteBucket = proxyManager.getProxy(key);
        assertTrue(remoteBucket.isPresent());

        // should return empty optional if bucket is removed
        removeBucketFromBackingStorage(key);
        remoteBucket = proxyManager.getProxy(key);
        assertFalse(remoteBucket.isPresent());
    }

    @Test
    public void testTryConsume() throws Exception {
        Function<Bucket, Long> action = bucket -> bucket.tryConsume(1)? 1L : 0L;
        Supplier<Bucket> bucketSupplier = () -> builderForLongRunningTests.build(key, THROW_BUCKET_NOT_FOUND_EXCEPTION);
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(15), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @Test
    public void testTryConsumeWithLimit() throws Exception {
        Function<Bucket, Long> action = bucket -> bucket.asScheduler().tryConsumeUninterruptibly(1, TimeUnit.MILLISECONDS.toNanos(50), UninterruptibleBlockingStrategy.PARKING) ? 1L : 0L;
        Supplier<Bucket> bucketSupplier = () -> builderForLongRunningTests.build(key, THROW_BUCKET_NOT_FOUND_EXCEPTION);
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(15), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @Test
    public void testTryConsumeAsync() throws Exception {
        if (!backend.getOptions().isAsyncModeSupported()) {
            return;
        }

        Function<Bucket, Long> action = bucket -> {
            try {
                return bucket.asAsync().tryConsume(1).get() ? 1L : 0L;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        };
        Supplier<Bucket> bucketSupplier = () -> builderForLongRunningTests.build(key, THROW_BUCKET_NOT_FOUND_EXCEPTION);
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(15), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @Test
    public void testTryConsumeAsyncWithLimit() throws Exception {
        if (!backend.getOptions().isAsyncModeSupported()) {
            return;
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Function<Bucket, Long> action = bucket -> {
            try {
                return bucket.asAsyncScheduler().tryConsume(1, TimeUnit.MILLISECONDS.toNanos(50), scheduler).get() ? 1L :0L;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        };
        Supplier<Bucket> bucketSupplier = () -> builderForLongRunningTests.build(key, THROW_BUCKET_NOT_FOUND_EXCEPTION);
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(15), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @Test
    public void testBucketRegistryWithKeyIndependentConfiguration() {
        BucketConfiguration configuration = Bucket4j.configurationBuilder(backend)
                .addLimit(Bandwidth.simple(10, Duration.ofDays(1)))
                .build();

        ProxyManager<String> registry = Bucket4j.proxyManager(backend);
        Bucket bucket1 = registry.getProxy(key, () -> configuration);
        assertTrue(bucket1.tryConsume(10));
        assertFalse(bucket1.tryConsume(1));

        Bucket bucket2 = registry.getProxy(anotherKey, () -> configuration);
        assertTrue(bucket2.tryConsume(10));
        assertFalse(bucket2.tryConsume(1));
    }

    @Test
    public void testBucketWithNotLazyConfiguration() {
        BucketConfiguration configuration = Bucket4j.configurationBuilder(backend)
                .addLimit(Bandwidth.simple(10, Duration.ofDays(1)))
                .build();

        ProxyManager<String> registry = Bucket4j.proxyManager(backend);
        Bucket bucket = registry.getProxy(key, configuration);
        assertTrue(bucket.tryConsume(10));
        assertFalse(bucket.tryConsume(1));
    }

}
