package io.github.bucket4j.tck;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.proxy.BucketNotFoundException;
import io.github.bucket4j.distributed.proxy.ExpiredEntriesCleaner;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.optimization.DelayParameters;
import io.github.bucket4j.distributed.proxy.optimization.NopeOptimizationListener;
import io.github.bucket4j.distributed.proxy.optimization.Optimization;
import io.github.bucket4j.distributed.proxy.optimization.Optimizations;
import io.github.bucket4j.distributed.proxy.optimization.PredictionParameters;
import io.github.bucket4j.distributed.proxy.optimization.delay.DelayOptimization;
import io.github.bucket4j.distributed.proxy.optimization.manual.ManuallySyncingOptimization;
import io.github.bucket4j.distributed.proxy.optimization.predictive.PredictiveOptimization;
import io.github.bucket4j.distributed.proxy.optimization.skiponzero.SkipSyncOnZeroOptimization;
import io.github.bucket4j.util.AsyncConsumptionScenario;
import io.github.bucket4j.util.ConsumptionScenario;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.github.bucket4j.distributed.proxy.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION;
import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractDistributedBucketTest {

    public static List<String> ADD_OPENS = Arrays.asList(
      "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
      "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
      "--add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED",
      "--add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED",
      "--add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED",
      "--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED",
      "--add-opens=java.base/java.io=ALL-UNNAMED",
      "--add-opens=java.base/java.nio=ALL-UNNAMED",
      "--add-opens=java.base/java.util=ALL-UNNAMED",
      "--add-opens=java.base/java.lang=ALL-UNNAMED"
    );

    protected static List<ProxyManagerSpec<?, ?, ?>> specs;

    public static Stream<ProxyManagerSpec<?, ?, ?>> specs() {
        return specs.stream();
    }

    private BucketConfiguration configurationForLongRunningTests = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)).withInitialTokens(0))
            .addLimit(Bandwidth.simple(200, Duration.ofSeconds(10)).withInitialTokens(0))
            .build();
    private double permittedRatePerSecond = Math.min(1_000d / 60, 200.0 / 10);

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testReconstructRecoveryStrategy(ProxyManagerSpec<K, P, B> spec) {
        K key = spec.generateRandomKey();
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(200, Duration.ofSeconds(10)))
                .build();

        ProxyManager<K> proxyManager = spec.builder.get().build();
        Bucket bucket = proxyManager.builder().build(key, configuration);

        assertTrue(bucket.tryConsume(1));

        // simulate crash
        proxyManager.removeProxy(key);

        assertTrue(bucket.tryConsume(1));
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testThrowExceptionRecoveryStrategy(ProxyManagerSpec<K, P, B> spec) {
        K key = spec.generateRandomKey();
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build();
        ProxyManager<K> proxyManager = spec.builder.get().build();
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

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testLocateConfigurationThroughProxyManager(ProxyManagerSpec<K, P, B> spec) {
        K key = spec.generateRandomKey();
        // should return empty optional if bucket is not stored
        ProxyManager<K> proxyManager = spec.builder.get().build();
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

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testBucketRemoval(ProxyManagerSpec<K, P, B> spec) {
        K key = spec.generateRandomKey();

        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(4, Duration.ofHours(1)))
                .build();
        ProxyManager<K> proxyManager = spec.builder.get().build();
        BucketProxy bucket = proxyManager.builder().build(key, configuration);
        bucket.getAvailableTokens();

        assertTrue(proxyManager.getProxyConfiguration(key).isPresent());
        proxyManager.removeProxy(key);
        assertFalse(proxyManager.getProxyConfiguration(key).isPresent());
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testNoExpirationAfterWrite(ProxyManagerSpec<K, P, B> spec) throws InterruptedException, SQLException {
        if (!spec.expirationSupported) {
            return;
        }
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds (1)))
            .build();
        ProxyManager<K> proxyManager = spec.builder.get()
                .expirationAfterWrite(ExpirationAfterWriteStrategy.none())
                .build();;

        K key = spec.generateRandomKey();
        BucketProxy bucket = proxyManager.builder().build(key, () -> configuration);
        assertEquals(10, bucket.tryConsumeAsMuchAsPossible());
        Thread.sleep(3000);
        if (proxyManager instanceof ExpiredEntriesCleaner cleaner) {
            assertEquals(0, cleaner.removeExpired(1));
        }
        assertTrue(proxyManager.getProxyConfiguration(key).isPresent());
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testNoExpirationAfterWrite_Async(ProxyManagerSpec<K, P, B> spec) throws InterruptedException, ExecutionException, SQLException {
        if (!spec.expirationSupported) {
            return;
        }
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds (1)))
            .build();
        ProxyManager<K> proxyManager = spec.builder.get()
                .expirationAfterWrite(ExpirationAfterWriteStrategy.none())
                .build();

        if (!proxyManager.isAsyncModeSupported()) {
            return;
        }

        K key = spec.generateRandomKey();
        AsyncBucketProxy bucket = proxyManager.asAsync().builder().build(key, () -> CompletableFuture.completedFuture(configuration));
        assertEquals(10, bucket.tryConsumeAsMuchAsPossible().get());
        Thread.sleep(3000);
        if (proxyManager instanceof ExpiredEntriesCleaner cleaner) {
            assertEquals(0, cleaner.removeExpired(1));
        }
        assertTrue(proxyManager.getProxyConfiguration(key).isPresent());
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testFixedTtlExpirationAfterWrite(ProxyManagerSpec<K, P, B> spec) throws InterruptedException, SQLException {
        if (!spec.expirationSupported) {
            return;
        }
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds (100)))
            .build();
        ProxyManager<K> proxyManager = spec.builder.get()
            .expirationAfterWrite(ExpirationAfterWriteStrategy.fixedTimeToLive(Duration.ofSeconds(1)))
            .build();

        K key = spec.generateRandomKey();
        BucketProxy bucket = proxyManager.builder().build(key, () -> configuration);
        assertEquals(10, bucket.tryConsumeAsMuchAsPossible());
        Thread.sleep(3000);
        if (proxyManager instanceof ExpiredEntriesCleaner cleaner) {
            assertEquals(1, cleaner.removeExpired(1));
        }
        assertTrue(proxyManager.getProxyConfiguration(key).isEmpty());
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testFixedTtlExpirationAfterWrite_Async(ProxyManagerSpec<K, P, B> spec) throws InterruptedException, ExecutionException, SQLException {
        if (!spec.expirationSupported) {
            return;
        }
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds (100)))
            .build();
        ProxyManager<K> proxyManager = spec.builder.get()
            .expirationAfterWrite(ExpirationAfterWriteStrategy.fixedTimeToLive(Duration.ofSeconds(1)))
            .build();
        if (!proxyManager.isAsyncModeSupported()) {
            return;
        }

        K key = spec.generateRandomKey();
        AsyncBucketProxy bucket = proxyManager.asAsync().builder().build(key, () -> CompletableFuture.completedFuture(configuration));
        assertEquals(10, bucket.tryConsumeAsMuchAsPossible().get());
        Thread.sleep(3000);
        if (proxyManager instanceof ExpiredEntriesCleaner cleaner) {
            assertEquals(1, cleaner.removeExpired(1));
        }
        assertTrue(proxyManager.getProxyConfiguration(key).isEmpty());
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testRefillBasedExpirationAfterWrite(ProxyManagerSpec<K, P, B> spec) throws InterruptedException, SQLException {
        if (!spec.expirationSupported) {
            return;
        }
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds (10)))
            .build();
        ProxyManager<K> proxyManager = spec.builder.get()
                .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(1)))
                .build();

        K key = spec.generateRandomKey();
        BucketProxy bucket = proxyManager.builder().build(key, () -> configuration);
        assertTrue(bucket.tryConsume(1));
        Thread.sleep(100);
        if (proxyManager instanceof ExpiredEntriesCleaner cleaner) {
            assertEquals(0, cleaner.removeExpired(1));
        }
        assertFalse(proxyManager.getProxyConfiguration(key).isEmpty());
        Thread.sleep(3000);
        if (proxyManager instanceof ExpiredEntriesCleaner cleaner) {
            assertEquals(1, cleaner.removeExpired(1));
        }
        assertTrue(proxyManager.getProxyConfiguration(key).isEmpty());
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testRefillBasedExpirationAfterWrite_Async(ProxyManagerSpec<K, P, B> spec) throws InterruptedException, ExecutionException, SQLException {
        if (!spec.expirationSupported) {
            return;
        }
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds (10)))
            .build();
        ProxyManager<K> proxyManager = spec.builder.get()
            .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(1)))
            .build();

        if (!proxyManager.isAsyncModeSupported()) {
            return;
        }

        K key = spec.generateRandomKey();
        AsyncBucketProxy bucket = proxyManager.asAsync().builder().build(key, () -> CompletableFuture.completedFuture(configuration));
        assertEquals(true, bucket.tryConsume(1).get());
        Thread.sleep(100);
        if (proxyManager instanceof ExpiredEntriesCleaner cleaner) {
            assertEquals(0, cleaner.removeExpired(1));
        }
        assertFalse(proxyManager.getProxyConfiguration(key).isEmpty());
        Thread.sleep(3000);
        if (proxyManager instanceof ExpiredEntriesCleaner cleaner) {
            assertEquals(1, cleaner.removeExpired(1));
        }
        assertTrue(proxyManager.getProxyConfiguration(key).isEmpty());
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testOptimizations(ProxyManagerSpec<K, P, B> spec) {
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds (1)))
            .build();

        TimeMeter clock = TimeMeter.SYSTEM_MILLISECONDS;

        DelayParameters delayParameters = new DelayParameters(1, Duration.ofNanos(1));
        List<Optimization> optimizations = Arrays.asList(
            Optimizations.batching(),
            new DelayOptimization(delayParameters, NopeOptimizationListener.INSTANCE, clock),
            new PredictiveOptimization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeOptimizationListener.INSTANCE, clock),
            new SkipSyncOnZeroOptimization(NopeOptimizationListener.INSTANCE, clock),
            new ManuallySyncingOptimization(NopeOptimizationListener.INSTANCE, clock)
        );

        for (Optimization optimization : optimizations) {
            try {
                K key = spec.generateRandomKey();
                ProxyManager<K> proxyManager = spec.builder.get().build();
                BucketProxy bucket = proxyManager.builder()
                    .withOptimization(optimization)
                    .build(key, configuration);

                assertEquals(10, bucket.getAvailableTokens());
                for (int i = 0; i < 5; i++) {
                    assertTrue(bucket.tryConsume(1));
                }
                proxyManager.removeProxy(key);

                bucket.forceAddTokens(90);
                assertEquals(100, bucket.getAvailableTokens());


                proxyManager.removeProxy(key);
                bucket.asVerbose().forceAddTokens(90);

                assertEquals(100, bucket.asVerbose().getAvailableTokens().getValue());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to check optimization " + optimization, e);
            }
        }
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testOptimizationsAsync(ProxyManagerSpec<K, P, B> spec) {
        ProxyManager<K> proxyManager = spec.builder.get().build();
        if (!proxyManager.isAsyncModeSupported()) {
            return;
        }

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds (1)))
            .build();

        TimeMeter clock = TimeMeter.SYSTEM_MILLISECONDS;

        DelayParameters delayParameters = new DelayParameters(1, Duration.ofNanos(1));
        List<Optimization> optimizations = Arrays.asList(
            Optimizations.batching(),
            new DelayOptimization(delayParameters, NopeOptimizationListener.INSTANCE, clock),
            new PredictiveOptimization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeOptimizationListener.INSTANCE, clock),
            new SkipSyncOnZeroOptimization(NopeOptimizationListener.INSTANCE, clock),
            new ManuallySyncingOptimization(NopeOptimizationListener.INSTANCE, clock)
        );

        for (Optimization optimization : optimizations) {
            try {
                K key = spec.generateRandomKey();
                AsyncBucketProxy bucket = proxyManager.asAsync().builder()
                    .withOptimization(optimization)
                    .build(key, configuration);

                assertEquals(10, bucket.getAvailableTokens().get());
                for (int i = 0; i < 5; i++) {
                    assertTrue(bucket.tryConsume(1).get());
                }
                proxyManager.removeProxy(key);

                bucket.forceAddTokens(90).get();
                assertEquals(100, bucket.getAvailableTokens().get());


                proxyManager.removeProxy(key);
                bucket.asVerbose().forceAddTokens(90).get();

                assertEquals(100, bucket.asVerbose().getAvailableTokens().get() .getValue());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to check optimization " + optimization, e);
            }
        }
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testParallelInitialization(ProxyManagerSpec<K, P, B> spec) throws InterruptedException {
        K key = spec.generateRandomKey();
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(1, Duration.ofMinutes(1))))
                .build();
        ProxyManager<K> proxyManager = spec.builder.get().build();

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

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testAsyncParallelInitialization(ProxyManagerSpec<K, P, B> spec) throws InterruptedException {
        ProxyManager<K> proxyManager = spec.builder.get().build();
        K key = spec.generateRandomKey();
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

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testParallelInitialization_withTimeout(ProxyManagerSpec<K, P, B> spec) throws InterruptedException {
        K key = spec.generateRandomKey();
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(10, Refill.intervally(1, Duration.ofMinutes(1))))
            .build();
        ProxyManager<K> proxyManager = spec.builder.get().requestTimeout(Duration.ofSeconds(3)).build();

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

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testAsyncParallelInitialization_withTimeout(ProxyManagerSpec<K, P, B> spec) throws InterruptedException {
        ProxyManager<K> proxyManager = spec.builder.get().requestTimeout(Duration.ofSeconds(3)).build();
        K key = spec.generateRandomKey();
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

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testUnconditionalConsume(ProxyManagerSpec<K, P, B> spec) throws Exception {
        ProxyManager<K> proxyManager = spec.builder.get().build();
        K key = spec.generateRandomKey();
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build();

        Bucket bucket = proxyManager.builder().build(key, () -> configuration);
        long overdraftNanos = bucket.consumeIgnoringRateLimits(121_000);
        assertEquals(overdraftNanos, TimeUnit.MINUTES.toNanos(120));
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testUnconditionalConsumeVerbose(ProxyManagerSpec<K, P, B> spec) throws Exception {
        ProxyManager<K> proxyManager = spec.builder.get().build();
        K key = spec.generateRandomKey();
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build();

        Bucket bucket = proxyManager.builder().build(key, () -> configuration);
        VerboseResult<Long> result = bucket.asVerbose().consumeIgnoringRateLimits(121_000);
        long overdraftNanos = result.getValue();

        assertEquals(overdraftNanos, TimeUnit.MINUTES.toNanos(120));
        assertEquals(configuration, result.getConfiguration());
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testTryConsume(ProxyManagerSpec<K, P, B> spec) throws Throwable {
        ProxyManager<K> proxyManager = spec.builder.get().build();
        K key = spec.generateRandomKey();
        Function<Bucket, Long> action = bucket -> bucket.tryConsume(1)? 1L : 0L;
        Supplier<Bucket> bucketSupplier = () -> proxyManager.builder()
                .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .build(key, configurationForLongRunningTests);
        int durationSeconds = System.getenv("CI") == null ? 5 : 1;
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(durationSeconds), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testTryConsumeWithLimit(ProxyManagerSpec<K, P, B> spec) throws Throwable {
        ProxyManager<K> proxyManager = spec.builder.get().build();
        K key = spec.generateRandomKey();
        Function<Bucket, Long> action = bucket -> bucket.asBlocking().tryConsumeUninterruptibly(1, TimeUnit.MILLISECONDS.toNanos(50), UninterruptibleBlockingStrategy.PARKING) ? 1L : 0L;
        Supplier<Bucket> bucketSupplier = () -> proxyManager.builder()
                .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .build(key, configurationForLongRunningTests);
        int durationSeconds = System.getenv("CI") == null ? 5 : 1;
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(durationSeconds), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testTryConsumeAsync(ProxyManagerSpec<K, P, B> spec) throws Exception {
        ProxyManager<K> proxyManager = spec.builder.get().build();
        if (!proxyManager.isAsyncModeSupported()) {
            return;
        }
        K key = spec.generateRandomKey();

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

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testTryConsumeAsyncWithLimit(ProxyManagerSpec<K, P, B> spec) throws Exception {
        ProxyManager<K> proxyManager = spec.builder.get().build();
        if (!proxyManager.isAsyncModeSupported()) {
            return;
        }

        K key = spec.generateRandomKey();
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

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testBucketRegistryWithKeyIndependentConfiguration(ProxyManagerSpec<K, P, B> spec) {
        ProxyManager<K> proxyManager = spec.builder.get().build();
        K key = spec.generateRandomKey();
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofDays(1)))
                .build();

        Bucket bucket1 = proxyManager.builder().build(key, configuration);
        assertTrue(bucket1.tryConsume(10));
        assertFalse(bucket1.tryConsume(1));

        K anotherKey = spec.generateRandomKey();
        Bucket bucket2 = proxyManager.builder().build(anotherKey, () -> configuration);
        assertTrue(bucket2.tryConsume(10));
        assertFalse(bucket2.tryConsume(1));
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testBucketWithNotLazyConfiguration(ProxyManagerSpec<K, P, B> spec) {
        ProxyManager<K> proxyManager = spec.builder.get().build();
        K key = spec.generateRandomKey();
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofDays(1)))
                .build();

        Bucket bucket = proxyManager.builder().build(key, configuration);
        assertTrue(bucket.tryConsume(10));
        assertFalse(bucket.tryConsume(1));
    }

    // https://github.com/bucket4j/bucket4j/issues/279
    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testVerboseBucket(ProxyManagerSpec<K, P, B> spec) {
        int MIN_CAPACITY = 4;
        int MAX_CAPACITY = 10;
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(MIN_CAPACITY, Refill.intervally(4, Duration.ofMinutes(20))))
                .addLimit(Bandwidth.classic(MAX_CAPACITY, Refill.intervally(10, Duration.ofMinutes(60))))
                .build();

        ProxyManager<K> proxyManager = spec.builder.get().build();
        K key = spec.generateRandomKey();
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

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testWithMapper(ProxyManagerSpec<K, P, B> spec) {
        K key = spec.generateRandomKey();
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofDays(1)))
                .build();

        ProxyManager<K> proxyManager = spec.builder.get().build();
        ProxyManager<String> mappedProxyManager = proxyManager.withMapper(dummy -> key);
        Bucket unmappedBucket = proxyManager.builder().build(key, configuration);
        Bucket mappedBucket = mappedProxyManager.builder().build("dummy", configuration);
        Bucket mappedBucket2 = mappedProxyManager.builder().build("dummy2", configuration);

        assertTrue(unmappedBucket.tryConsume(10));
        assertFalse(mappedBucket.tryConsume(1));
        assertFalse(mappedBucket2.tryConsume(1));

        unmappedBucket.reset();

        assertTrue(mappedBucket.tryConsume(5));
        assertTrue(mappedBucket2.tryConsume(5));
        assertFalse(unmappedBucket.tryConsume(1));
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testWithMapperAsync(ProxyManagerSpec<K, P, B> spec) throws Exception {
        ProxyManager<K> proxyManager = spec.builder.get().build();
        if (!proxyManager.isAsyncModeSupported()) {
            return;
        }

        K key = spec.generateRandomKey();
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofDays(1)))
                .build();

        ProxyManager<String> mappedProxyManager = proxyManager.withMapper(dummy -> key);
        AsyncBucketProxy unmappedBucket = proxyManager.asAsync().builder().build(key, configuration);
        AsyncBucketProxy mappedBucket = mappedProxyManager.asAsync().builder().build("dummy", configuration);
        AsyncBucketProxy mappedBucket2 = mappedProxyManager.asAsync().builder().build("dummy2", configuration);

        assertTrue(unmappedBucket.tryConsume(10).get());
        assertFalse(mappedBucket.tryConsume(1).get());
        assertFalse(mappedBucket2.tryConsume(1).get());

        unmappedBucket.reset().get();

        assertTrue(mappedBucket.tryConsume(5).get());
        assertTrue(mappedBucket2.tryConsume(5).get());
        assertFalse(unmappedBucket.tryConsume(1).get());
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void test_1000_tokens_consumption(ProxyManagerSpec<K, P, B> spec) throws InterruptedException {
        ProxyManager<K> proxyManager = spec.builder.get().build();
        K key = spec.generateRandomKey();
        int threadCount = 8;
        int opsCount = 1_000;
        int capacity = 2_000;

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(capacity).refillIntervally(1, Duration.ofDays(1)))
            .build();

        CountDownLatch startLatch = new CountDownLatch(threadCount);
        CountDownLatch stopLatch = new CountDownLatch(threadCount);
        AtomicInteger opsCounter = new AtomicInteger(opsCount);
        ConcurrentHashMap<Integer, Integer> updatesByThread = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, Throwable> errors = new ConcurrentHashMap<>();
        for (int i = 0; i < threadCount; i++) {
            Bucket bucket = proxyManager.builder().build(key, () -> configuration);
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    while (opsCounter.decrementAndGet() >= 0) {
                        Integer currenValue = null;
                        if (bucket.tryConsume(1)) {
                            updatesByThread.compute(threadId, (k, current) -> current == null ? 1 : current + 1);
                        } else {
                            throw new IllegalStateException("Token should be consumed");
                        }
                    }
                } catch (Throwable e) {
                    errors.put(threadId, e);
                    e.printStackTrace();
                } finally {
                    stopLatch.countDown();
                }
            }, "Updater-thread-" + i).start();
        }
        stopLatch.await();

        long availableTokens = proxyManager.builder().build(key, () -> configuration).getAvailableTokens();
        System.out.println("availableTokens " + availableTokens);
        System.out.println("Failed threads " + errors.keySet());
        System.out.println("Updates by thread " + updatesByThread);
        assertTrue(errors.isEmpty());
        assertEquals(capacity - opsCount, availableTokens);
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void test_1000_tokens_consumption_async(ProxyManagerSpec<K, P, B> spec) throws InterruptedException {
        ProxyManager<K> proxyManager = spec.builder.get().build();
        if (!proxyManager.isAsyncModeSupported()) {
            return;
        }

        K key = spec.generateRandomKey();
        int threadCount = 8;
        int opsCount = 1_000;
        int capacity = 2_000;

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(capacity).refillIntervally(1, Duration.ofDays(1)))
            .build();

        CountDownLatch startLatch = new CountDownLatch(threadCount);
        CountDownLatch stopLatch = new CountDownLatch(threadCount);
        AtomicInteger opsCounter = new AtomicInteger(opsCount);
        ConcurrentHashMap<Integer, Integer> updatesByThread = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, Throwable> errors = new ConcurrentHashMap<>();
        for (int i = 0; i < threadCount; i++) {
            AsyncBucketProxy bucket = proxyManager.asAsync().builder().build(key, () -> CompletableFuture.completedFuture(configuration));
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    while (opsCounter.decrementAndGet() >= 0) {
                        if (bucket.tryConsume(1).get()) {
                            updatesByThread.compute(threadId, (k, current) -> current == null ? 1 : current + 1);
                        } else {
                            throw new IllegalStateException("Token should be consumed");
                        }
                    }
                } catch (Throwable e) {
                    errors.put(threadId, e);
                    e.printStackTrace();
                } finally {
                    stopLatch.countDown();
                }
            }, "Updater-thread-" + i).start();
        }
        stopLatch.await();

        long availableTokens = proxyManager.builder().build(key, () -> configuration).getAvailableTokens();
        System.out.println("availableTokens " + availableTokens);
        System.out.println("Failed threads " + errors.keySet());
        System.out.println("Updates by thread " + updatesByThread);
        assertTrue(errors.isEmpty());
        assertEquals(capacity - opsCount, availableTokens);
    }

}
