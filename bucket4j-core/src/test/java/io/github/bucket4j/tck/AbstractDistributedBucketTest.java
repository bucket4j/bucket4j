package io.github.bucket4j.tck;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.BucketNotFoundException;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.util.AsyncConsumptionScenario;
import io.github.bucket4j.util.ConsumptionScenario;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.*;
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

        Bucket bucket = proxyManager.builder().build(key, configuration);

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
                .build(key, configuration);

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
                .build(key, configuration)
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
        BucketProxy bucket = proxyManager.builder().build(key, configuration);
        bucket.getAvailableTokens();

        assertTrue(proxyManager.getProxyConfiguration(key).isPresent());
        proxyManager.removeProxy(key);
        assertFalse(proxyManager.getProxyConfiguration(key).isPresent());
    }

    @Test
    public void testParallelInitialization() throws InterruptedException {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(1, Duration.ofMinutes(1))))
                .build();

        int PARALLELISM = 4;
        CountDownLatch startLatch = new CountDownLatch(PARALLELISM);
        CountDownLatch stopLatch = new CountDownLatch(PARALLELISM);
        for (int i = 0; i < PARALLELISM; i++) {
            new Thread(() -> {
                startLatch.countDown();
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    proxyManager.builder().build(key, () -> configuration).tryConsume(1);
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    stopLatch.countDown();
                }
            }).start();
        }
        stopLatch.await();

        BucketProxy bucket = proxyManager.builder().build(key, () -> configuration);
        assertEquals(10 - PARALLELISM, bucket.getAvailableTokens());
    }

    @Test
    public void testAsyncParallelInitialization() throws InterruptedException {
        if (!proxyManager.isAsyncModeSupported()) {
            return;
        }

        final BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(1, Duration.ofMinutes(1))))
                .build();

        int PARALLELISM = 4;
        CountDownLatch startLatch = new CountDownLatch(PARALLELISM);
        CountDownLatch stopLatch = new CountDownLatch(PARALLELISM);
        for (int i = 0; i < PARALLELISM; i++) {
            new Thread(() -> {
                startLatch.countDown();
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    try {
                        proxyManager.asAsync().builder().build(key, () -> CompletableFuture.completedFuture(configuration)).tryConsume(1).get();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                } finally {
                    stopLatch.countDown();
                }
            }).start();
        }
        stopLatch.await();

        BucketProxy bucket = proxyManager.builder().build(key, () -> configuration);
        assertEquals(10 - PARALLELISM, bucket.getAvailableTokens());
    }

    @Test
    public void testUnconditionalConsume() throws Exception {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build();

        Bucket bucket = proxyManager.builder().build(key, () -> configuration);
        long overdraftNanos = bucket.consumeIgnoringRateLimits(121_000);
        assertEquals(overdraftNanos, TimeUnit.MINUTES.toNanos(120));
    }

    @Test
    public void testUnconditionalConsumeVerbose() throws Exception {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build();

        Bucket bucket = proxyManager.builder().build(key, () -> configuration);
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
                .build(key, configurationForLongRunningTests);
        int durationSeconds = System.getenv("CI") == null ? 5 : 1;
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(durationSeconds), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @Test
    public void testTryConsumeWithLimit() throws Throwable {
        Function<Bucket, Long> action = bucket -> bucket.asBlocking().tryConsumeUninterruptibly(1, TimeUnit.MILLISECONDS.toNanos(50), UninterruptibleBlockingStrategy.PARKING) ? 1L : 0L;
        Supplier<Bucket> bucketSupplier = () -> proxyManager.builder()
                .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .build(key, configurationForLongRunningTests);
        int durationSeconds = System.getenv("CI") == null ? 5 : 1;
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(durationSeconds), bucketSupplier, action, permittedRatePerSecond);
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
                .build(key, configurationForLongRunningTests);
        int durationSeconds = System.getenv("CI") == null ? 5 : 1;
        AsyncConsumptionScenario scenario = new AsyncConsumptionScenario(4, TimeUnit.SECONDS.toNanos(durationSeconds), bucketSupplier, action, permittedRatePerSecond);
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
                .build(key, configurationForLongRunningTests);
        int durationSeconds = System.getenv("CI") == null ? 5 : 1;
        AsyncConsumptionScenario scenario = new AsyncConsumptionScenario(4, TimeUnit.SECONDS.toNanos(durationSeconds), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @Test
    public void testBucketRegistryWithKeyIndependentConfiguration() {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofDays(1)))
                .build();

        Bucket bucket1 = proxyManager.builder().build(key, configuration);
        assertTrue(bucket1.tryConsume(10));
        assertFalse(bucket1.tryConsume(1));

        Bucket bucket2 = proxyManager.builder().build(anotherKey, () -> configuration);
        assertTrue(bucket2.tryConsume(10));
        assertFalse(bucket2.tryConsume(1));
    }

    @Test
    public void testBucketWithNotLazyConfiguration() {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofDays(1)))
                .build();

        Bucket bucket = proxyManager.builder().build(key, configuration);
        assertTrue(bucket.tryConsume(10));
        assertFalse(bucket.tryConsume(1));
    }

    // https://github.com/bucket4j/bucket4j/issues/279
    @Test
    public void testVerboseBucket() {
        int MIN_CAPACITY = 4;
        int MAX_CAPACITY = 10;
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(MIN_CAPACITY, Refill.intervally(4, Duration.ofMinutes(20))))
                .addLimit(Bandwidth.classic(MAX_CAPACITY, Refill.intervally(10, Duration.ofMinutes(60))))
                .build();

        K key = generateRandomKey();
        Bucket bucket = proxyManager.builder().build(key, configuration);

        for (int i = 1; i <= 4; i++) {
            VerboseResult<ConsumptionProbe> verboseResult = bucket.asVerbose().tryConsumeAndReturnRemaining(1);
            ConsumptionProbe probe = verboseResult.getValue();
            long[] availableTokensPerEachBandwidth = verboseResult.getDiagnostics().getAvailableTokensPerEachBandwidth();
            System.out.println("Remaining tokens = " + probe.getRemainingTokens());
            System.out.println("Tokens per bandwidth = " + Arrays.toString(availableTokensPerEachBandwidth));
            assertEquals(MIN_CAPACITY - i, probe.getRemainingTokens());
            assertEquals(MIN_CAPACITY - i, availableTokensPerEachBandwidth[0]);
            assertEquals(MAX_CAPACITY - i, availableTokensPerEachBandwidth[1]);
        }
    }

}
