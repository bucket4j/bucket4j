package io.github.bucket4j.grid.jcache;


import io.github.bucket4j.*;
import io.github.bucket4j.grid.BucketNotFoundException;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.util.ConsumptionScenario;
import org.junit.ClassRule;
import org.junit.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.github.bucket4j.grid.RecoveryStrategy.RECONSTRUCT;
import static io.github.bucket4j.grid.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class AbstractJCacheTest {

    @ClassRule
    public static JCacheFixture jCacheFixture = new JCacheFixture();

    private final String key = UUID.randomUUID().toString();
    private final String onotherKey = UUID.randomUUID().toString();

    private JCacheBucketBuilder builder = Bucket4j.extension(JCache.class).builder()
            .addLimit(0, Bandwidth.simple(1_000, Duration.ofMinutes(1)))
            .addLimit(0, Bandwidth.simple(200, Duration.ofSeconds(10)));
    private double permittedRatePerSecond = Math.min(1_000d / 60, 200.0 / 10);

    @Test
    public void testReconstructRecoveryStrategy() {
        Bucket bucket = Bucket4j.extension(JCache.class).builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(200, Duration.ofSeconds(10)))
                .build(jCacheFixture.getCache(), key, RECONSTRUCT);

        assertTrue(bucket.tryConsume(1));

        // simulate crash
        jCacheFixture.getCache().remove(key);

        assertTrue(bucket.tryConsume(1));
    }

    @Test
    public void testThrowExceptionRecoveryStrategy() {
        Bucket bucket = Bucket4j.extension(JCache.class).builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(200, Duration.ofSeconds(10)))
                .build(jCacheFixture.getCache(), key, THROW_BUCKET_NOT_FOUND_EXCEPTION);

        assertTrue(bucket.tryConsume(1));

        // simulate crash
        jCacheFixture.getCache().remove(key);

        try {
            bucket.tryConsume(1);
            fail();
        } catch (BucketNotFoundException e) {
            // ok
        }
    }

    @Test
    public void testTryConsume() throws Exception {
        Function<Bucket, Long> action = bucket -> bucket.tryConsume(1)? 1L : 0L;
        Supplier<Bucket> bucketSupplier = () -> builder.build(jCacheFixture.getCache(), key, THROW_BUCKET_NOT_FOUND_EXCEPTION);
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(15), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @Test
    public void testConsume() throws Exception {
        Function<Bucket, Long> action = bucket -> {
            bucket.consumeUninterruptibly(1, BlockingStrategy.PARKING);
            return 1L;
        };
        Supplier<Bucket> bucketSupplier = () -> builder.build(jCacheFixture.getCache(), key, THROW_BUCKET_NOT_FOUND_EXCEPTION);
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(15), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @Test
    public void testJCacheBucketRegistryWithKeyIndependentConfiguration() {
        BucketConfiguration configuration = Bucket4j.configurationBuilder()
                .addLimit(Bandwidth.simple(10, Duration.ofDays(1)))
                .buildConfiguration();

        ProxyManager<String> registry = Bucket4j.extension(JCache.class).proxyManagerForCache(jCacheFixture.getCache());
        Bucket bucket1 = registry.getProxy(key, () -> configuration);
        assertTrue(bucket1.tryConsume(10));
        assertFalse(bucket1.tryConsume(1));

        Bucket bucket2 = registry.getProxy(onotherKey, () -> configuration);
        assertTrue(bucket2.tryConsume(10));
        assertFalse(bucket2.tryConsume(1));
    }

}
