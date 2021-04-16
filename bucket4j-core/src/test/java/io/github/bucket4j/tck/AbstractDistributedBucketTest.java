package io.github.bucket4j.tck;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.BucketNotFoundException;
import io.github.bucket4j.util.AsyncConsumptionScenario;
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

import static io.github.bucket4j.distributed.proxy.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION;
import static org.junit.Assert.*;

public abstract class AbstractDistributedBucketTest<K> {

    private final K key = generateRandomKey();
    private final K anotherKey = generateRandomKey();
    private final ProxyManager<K> proxyManager = getProxyManager();

    private BucketConfiguration configurationForLongRunningTests = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)).withInitialTokens(0))
            .addLimit(Bandwidth.simple(200, Duration.ofSeconds(10)).withInitialTokens(0))
            .build();
    private double permittedRatePerSecond = Math.min(1_000d / 60, 200.0 / 10);

    protected abstract ProxyManager<K> getProxyManager();

    protected abstract K generateRandomKey();

    @Test
    public void testReconstructRecoveryStrategy() {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(200, Duration.ofSeconds(10)))
                .build();

        Bucket bucket = proxyManager.builder().buildProxy(key, configuration);

        assertTrue(bucket.tryConsume(1));

        // simulate crash
        proxyManager.removeProxy(key);

        assertTrue(bucket.tryConsume(1));
    }

    @Test
    public void testThrowExceptionRecoveryStrategy() {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build();
        Bucket bucket = proxyManager.builder()
                .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .buildProxy(key, configuration);

        assertTrue(bucket.tryConsume(1));

        // simulate crash
        proxyManager.removeProxy(key);

        try {
            bucket.tryConsume(1);
            fail();
        } catch (BucketNotFoundException e) {
            // ok
        }
    }

    @Test
    public void testLocateConfigurationThroughProxyManager() {
        // should return empty optional if bucket is not stored
        Optional<BucketConfiguration> remoteConfiguration = proxyManager.getProxyConfiguration(key);
        assertFalse(remoteConfiguration.isPresent());

        // should return not empty options if bucket is stored
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build();
        proxyManager.builder()
                .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .buildProxy(key, configuration)
                .getAvailableTokens();
        remoteConfiguration = proxyManager.getProxyConfiguration(key);
        assertTrue(remoteConfiguration.isPresent());

        // should return empty optional if bucket is removed
        proxyManager.removeProxy(key);
        remoteConfiguration = proxyManager.getProxyConfiguration(key);
        assertFalse(remoteConfiguration.isPresent());
    }

    @Test
    public void testBucketRemoval() {
        K key = generateRandomKey();

        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(4, Duration.ofHours(1)))
                .build();
        BucketProxy bucket = proxyManager.builder().buildProxy(key, configuration);
        bucket.getAvailableTokens();

        assertTrue(proxyManager.getProxyConfiguration(key).isPresent());
        proxyManager.removeProxy(key);
        assertFalse(proxyManager.getProxyConfiguration(key).isPresent());
    }

    @Test
    public void testUnconditionalConsume() throws Exception {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build();

        Bucket bucket = proxyManager.builder().buildProxy(key, () -> configuration);
        long overdraftNanos = bucket.consumeIgnoringRateLimits(121_000);
        assertEquals(overdraftNanos, TimeUnit.MINUTES.toNanos(120));
    }

    @Test
    public void testUnconditionalConsumeVerbose() throws Exception {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build();

        Bucket bucket = proxyManager.builder().buildProxy(key, () -> configuration);
        VerboseResult<Long> result = bucket.asVerbose().consumeIgnoringRateLimits(121_000);
        long overdraftNanos = result.getValue();

        assertEquals(overdraftNanos, TimeUnit.MINUTES.toNanos(120));
        assertEquals(configuration, result.getConfiguration());
    }

    @Test
    public void testTryConsume() throws Throwable {
        Function<Bucket, Long> action = bucket -> bucket.tryConsume(1)? 1L : 0L;
        Supplier<Bucket> bucketSupplier = () -> proxyManager.builder()
                .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .buildProxy(key, configurationForLongRunningTests);
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(5), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @Test
    public void testTryConsumeWithLimit() throws Throwable {
        Function<Bucket, Long> action = bucket -> bucket.asBlocking().tryConsumeUninterruptibly(1, TimeUnit.MILLISECONDS.toNanos(50), UninterruptibleBlockingStrategy.PARKING) ? 1L : 0L;
        Supplier<Bucket> bucketSupplier = () -> proxyManager.builder()
                .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .buildProxy(key, configurationForLongRunningTests);
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(5), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @Test
    public void testTryConsumeAsync() throws Exception {
        if (!proxyManager.isAsyncModeSupported()) {
            return;
        }

        Function<AsyncBucketProxy, Long> action = bucket -> {
            try {
                return bucket.tryConsume(1).get() ? 1L : 0L;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        };
        Supplier<AsyncBucketProxy> bucketSupplier = () -> proxyManager.asAsync().builder()
                .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .buildProxy(key, configurationForLongRunningTests);
        AsyncConsumptionScenario scenario = new AsyncConsumptionScenario(4, TimeUnit.SECONDS.toNanos(5), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @Test
    public void testTryConsumeAsyncWithLimit() throws Exception {
        if (!proxyManager.isAsyncModeSupported()) {
            return;
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Function<AsyncBucketProxy, Long> action = bucket -> {
            try {
                return bucket.asScheduler().tryConsume(1, TimeUnit.MILLISECONDS.toNanos(50), scheduler).get() ? 1L :0L;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        };
        Supplier<AsyncBucketProxy> bucketSupplier = () -> proxyManager.asAsync().builder()
                .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .buildProxy(key, configurationForLongRunningTests);
        AsyncConsumptionScenario scenario = new AsyncConsumptionScenario(4, TimeUnit.SECONDS.toNanos(5), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @Test
    public void testBucketRegistryWithKeyIndependentConfiguration() {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofDays(1)))
                .build();

        Bucket bucket1 = proxyManager.builder().buildProxy(key, configuration);
        assertTrue(bucket1.tryConsume(10));
        assertFalse(bucket1.tryConsume(1));

        Bucket bucket2 = proxyManager.builder().buildProxy(anotherKey, () -> configuration);
        assertTrue(bucket2.tryConsume(10));
        assertFalse(bucket2.tryConsume(1));
    }

    @Test
    public void testBucketWithNotLazyConfiguration() {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofDays(1)))
                .build();

        Bucket bucket = proxyManager.builder().buildProxy(key, configuration);
        assertTrue(bucket.tryConsume(10));
        assertFalse(bucket.tryConsume(1));
    }

}
