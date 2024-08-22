package io.github.bucket4j.tck;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.UninterruptibleBlockingStrategy;
import io.github.bucket4j.VerboseResult;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.AbstractAsyncProxyManagerBuilder;
import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.distributed.proxy.ExpiredEntriesCleaner;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronization;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronizations;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.DelayParameters;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.NopeBucketSynchronizationListener;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.PredictionParameters;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.delay.DelayBucketSynchronization;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.manual.ManuallySyncingBucketSynchronization;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.predictive.PredictiveBucketSynchronization;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.skiponzero.SkipSyncOnZeroBucketSynchronization;
import io.github.bucket4j.util.AsyncConsumptionScenario;
import io.github.bucket4j.util.ConsumptionScenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private static final AsyncProxyManagerSpec NOPE = new AsyncProxyManagerSpec("Nope", null, null);
    protected static List<ProxyManagerSpec<?, ?, ?>> specs;
    protected static List<AsyncProxyManagerSpec<?, ?, ?>> asyncSpecs = new ArrayList<>();

    public static Stream<ProxyManagerSpec<?, ?, ?>> specs() {
        return specs.stream();
    }

    public static Stream<AsyncProxyManagerSpec<?, ?, ?>> asyncSpecs() {
        return asyncSpecs.isEmpty() ? Stream.of(NOPE) : asyncSpecs.stream();
    }

    private BucketConfiguration configurationForLongRunningTests = BucketConfiguration.builder()
        .addLimit(Bandwidth.builder().capacity(1_000).refillGreedy(1_000, Duration.ofMinutes(1)).initialTokens(0).build())
        .addLimit(Bandwidth.builder().capacity(200).refillGreedy(200, Duration.ofSeconds(10)).initialTokens(0).build())
        .build();
    private double permittedRatePerSecond = Math.min(1_000d / 60, 200.0 / 10);

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
                .addLimit(Bandwidth.builder().capacity(1_000).refillGreedy(1_000, Duration.ofMinutes(1)).build())
                .build();
        proxyManager.builder()
                .build(key, () -> configuration)
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
                .addLimit(Bandwidth.builder().capacity(4).refillGreedy(4, Duration.ofHours(1)).build())
                .build();
        ProxyManager<K> proxyManager = spec.builder.get().build();
        BucketProxy bucket = proxyManager.builder().build(key, () -> configuration);
        bucket.getAvailableTokens();

        assertTrue(proxyManager.getProxyConfiguration(key).isPresent());
        proxyManager.removeProxy(key);
        assertFalse(proxyManager.getProxyConfiguration(key).isPresent());
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testNoExpirationAfterWrite(ProxyManagerSpec<K, P, B> spec) throws InterruptedException {
        if (!spec.expirationSupported) {
            return;
        }
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofSeconds(1)).build())
            .build();
        ProxyManager<K> proxyManager = spec.builder.get()
                .expirationAfterWrite(ExpirationAfterWriteStrategy.none())
                .build();

        K key = spec.generateRandomKey();
        BucketProxy bucket = proxyManager.builder().build(key, () -> configuration);
        assertEquals(10, bucket.tryConsumeAsMuchAsPossible());
        Thread.sleep(3000);
        if (proxyManager instanceof ExpiredEntriesCleaner cleaner) {
            assertEquals(0, cleaner.removeExpired(1));
        }
        assertTrue(proxyManager.getProxyConfiguration(key).isPresent());
    }

    @MethodSource("asyncSpecs")
    @ParameterizedTest
    public <K, P extends AsyncProxyManager<K>, B extends AbstractAsyncProxyManagerBuilder<K, P, B>> void testNoExpirationAfterWrite_Async(AsyncProxyManagerSpec<K, P, B> spec) throws InterruptedException, ExecutionException {
        if (spec == NOPE || !spec.expirationSupported) {
            return;
        }
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofSeconds(1)).build())
            .build();
        AsyncProxyManager<K> proxyManager = spec.builder.get()
                .expirationAfterWrite(ExpirationAfterWriteStrategy.none())
                .build();

        K key = spec.generateRandomKey();
        AsyncBucketProxy bucket = proxyManager.builder().build(key, () -> CompletableFuture.completedFuture(configuration));
        assertEquals(10, bucket.tryConsumeAsMuchAsPossible().get());
        Thread.sleep(3000);
        if (proxyManager instanceof ExpiredEntriesCleaner cleaner) {
            assertEquals(0, cleaner.removeExpired(1));
        }
        assertTrue(proxyManager.getProxyConfiguration(key).get().isPresent());
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testFixedTtlExpirationAfterWrite(ProxyManagerSpec<K, P, B> spec) throws InterruptedException {
        if (!spec.expirationSupported) {
            return;
        }
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofSeconds(100)).build())
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

    @MethodSource("asyncSpecs")
    @ParameterizedTest
    public <K, P extends AsyncProxyManager<K>, B extends AbstractAsyncProxyManagerBuilder<K, P, B>> void testFixedTtlExpirationAfterWrite_Async(AsyncProxyManagerSpec<K, P, B> spec) throws InterruptedException, ExecutionException {
        if (spec == NOPE || !spec.expirationSupported) {
            return;
        }
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofSeconds(100)).build())
            .build();
        AsyncProxyManager<K> proxyManager = spec.builder.get()
            .expirationAfterWrite(ExpirationAfterWriteStrategy.fixedTimeToLive(Duration.ofSeconds(1)))
            .build();

        K key = spec.generateRandomKey();
        AsyncBucketProxy bucket = proxyManager.builder().build(key, () -> CompletableFuture.completedFuture(configuration));
        assertEquals(10, bucket.tryConsumeAsMuchAsPossible().get());
        Thread.sleep(3000);
        if (proxyManager instanceof ExpiredEntriesCleaner cleaner) {
            assertEquals(1, cleaner.removeExpired(1));
        }
        assertTrue(proxyManager.getProxyConfiguration(key).get().isEmpty());
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testRefillBasedExpirationAfterWrite(ProxyManagerSpec<K, P, B> spec) throws InterruptedException {
        if (!spec.expirationSupported) {
            return;
        }
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofSeconds(10)).build())
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

    @MethodSource("asyncSpecs")
    @ParameterizedTest
    public <K, P extends AsyncProxyManager<K>, B extends AbstractAsyncProxyManagerBuilder<K, P, B>> void testRefillBasedExpirationAfterWrite_Async(AsyncProxyManagerSpec<K, P, B> spec) throws InterruptedException, ExecutionException {
        if (spec == NOPE || !spec.expirationSupported) {
            return;
        }
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofSeconds(10)).build())
            .build();
        AsyncProxyManager<K> proxyManager = spec.builder.get()
            .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(1)))
            .build();

        K key = spec.generateRandomKey();
        AsyncBucketProxy bucket = proxyManager.builder().build(key, () -> CompletableFuture.completedFuture(configuration));
        assertEquals(true, bucket.tryConsume(1).get());
        Thread.sleep(100);
        if (proxyManager instanceof ExpiredEntriesCleaner cleaner) {
            assertEquals(0, cleaner.removeExpired(1));
        }
        assertFalse(proxyManager.getProxyConfiguration(key).get().isEmpty());
        Thread.sleep(3000);
        if (proxyManager instanceof ExpiredEntriesCleaner cleaner) {
            assertEquals(1, cleaner.removeExpired(1));
        }
        assertTrue(proxyManager.getProxyConfiguration(key).get().isEmpty());
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testSynchronizations(ProxyManagerSpec<K, P, B> spec) {
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofSeconds(1)).build())
            .build();

        TimeMeter clock = TimeMeter.SYSTEM_MILLISECONDS;

        DelayParameters delayParameters = new DelayParameters(1, Duration.ofNanos(1));
        List<BucketSynchronization> bucketSynchronizations = Arrays.asList(
            BucketSynchronizations.batching(),
            new DelayBucketSynchronization(delayParameters, NopeBucketSynchronizationListener.INSTANCE, clock),
            new PredictiveBucketSynchronization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeBucketSynchronizationListener.INSTANCE, clock),
            new SkipSyncOnZeroBucketSynchronization(NopeBucketSynchronizationListener.INSTANCE, clock),
            new ManuallySyncingBucketSynchronization(NopeBucketSynchronizationListener.INSTANCE, clock)
        );

        for (BucketSynchronization bucketSynchronization : bucketSynchronizations) {
            try {
                K key = spec.generateRandomKey();
                ProxyManager<K> proxyManager = spec.builder.get().build();
                BucketProxy bucket = proxyManager.builder()
                    .withSynchronization(bucketSynchronization)
                    .build(key, () -> configuration);

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
                throw new IllegalStateException("Failed to check synchronization " + bucketSynchronization, e);
            }
        }
    }

    @MethodSource("asyncSpecs")
    @ParameterizedTest
    public <K, P extends AsyncProxyManager<K>, B extends AbstractAsyncProxyManagerBuilder<K, P, B>> void testSynchronizationsAsync(AsyncProxyManagerSpec<K, P, B> spec) {
        if (spec == NOPE) {
            return;
        }
        AsyncProxyManager<K> proxyManager = spec.builder.get().build();

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofSeconds(1)).build())
            .build();

        TimeMeter clock = TimeMeter.SYSTEM_MILLISECONDS;

        DelayParameters delayParameters = new DelayParameters(1, Duration.ofNanos(1));
        List<BucketSynchronization> bucketSynchronizations = Arrays.asList(
            BucketSynchronizations.batching(),
            new DelayBucketSynchronization(delayParameters, NopeBucketSynchronizationListener.INSTANCE, clock),
            new PredictiveBucketSynchronization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeBucketSynchronizationListener.INSTANCE, clock),
            new SkipSyncOnZeroBucketSynchronization(NopeBucketSynchronizationListener.INSTANCE, clock),
            new ManuallySyncingBucketSynchronization(NopeBucketSynchronizationListener.INSTANCE, clock)
        );

        for (BucketSynchronization bucketSynchronization : bucketSynchronizations) {
            try {
                K key = spec.generateRandomKey();
                AsyncBucketProxy bucket = proxyManager.builder()
                    .withSynchronization(bucketSynchronization)
                    .build(key, () -> CompletableFuture.completedFuture(configuration));

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
                throw new IllegalStateException("Failed to check synchronization " + bucketSynchronization, e);
            }
        }
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testParallelInitialization(ProxyManagerSpec<K, P, B> spec) throws InterruptedException {
        K key = spec.generateRandomKey();
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillIntervally(1, Duration.ofMinutes(1)))
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

    @MethodSource("asyncSpecs")
    @ParameterizedTest
    public <K, P extends AsyncProxyManager<K>, B extends AbstractAsyncProxyManagerBuilder<K, P, B>> void testAsyncParallelInitialization(AsyncProxyManagerSpec<K, P, B> spec) throws InterruptedException, ExecutionException {
        if (spec == NOPE) {
            return;
        }
        AsyncProxyManager<K> proxyManager = spec.builder.get().build();
        K key = spec.generateRandomKey();

        final BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillIntervally(1, Duration.ofMinutes(1)))
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
                        proxyManager.builder().build(key, () -> CompletableFuture.completedFuture(configuration)).tryConsume(1).get();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                } finally {
                    stopLatch.countDown();
                }
            }).start();
        }
        stopLatch.await();

        AsyncBucketProxy bucket = proxyManager.builder().build(key, () -> CompletableFuture.completedFuture(configuration));
        assertEquals(10 - PARALLELISM, bucket.getAvailableTokens().get());
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testParallelInitialization_withTimeout(ProxyManagerSpec<K, P, B> spec) throws InterruptedException {
        K key = spec.generateRandomKey();
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillIntervally(1, Duration.ofMinutes(1)))
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

    @MethodSource("asyncSpecs")
    @ParameterizedTest
    public <K, P extends AsyncProxyManager<K>, B extends AbstractAsyncProxyManagerBuilder<K, P, B>> void testAsyncParallelInitialization_withTimeout(AsyncProxyManagerSpec<K, P, B> spec) throws InterruptedException, ExecutionException {
        if (spec == NOPE) {
            return;
        }
        AsyncProxyManager<K> proxyManager = spec.builder.get().requestTimeout(Duration.ofSeconds(3)).build();
        K key = spec.generateRandomKey();

        final BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillIntervally(1, Duration.ofMinutes(1)))
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
                        proxyManager.builder().build(key, () -> CompletableFuture.completedFuture(configuration)).tryConsume(1).get();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                } finally {
                    stopLatch.countDown();
                }
            }).start();
        }
        stopLatch.await();

        AsyncBucketProxy bucket = proxyManager.builder().build(key, () -> CompletableFuture.completedFuture(configuration));
        assertEquals(10 - PARALLELISM, bucket.getAvailableTokens().get());
    }

    @MethodSource("specs")
    @ParameterizedTest
    public <K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> void testUnconditionalConsume(ProxyManagerSpec<K, P, B> spec) throws Exception {
        ProxyManager<K> proxyManager = spec.builder.get().build();
        K key = spec.generateRandomKey();
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder().capacity(1_000).refillGreedy(1_000, Duration.ofMinutes(1)).build())
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
                .addLimit(Bandwidth.builder().capacity(1_000).refillGreedy(1_000, Duration.ofMinutes(1)).build())
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
                .build(key, () -> configurationForLongRunningTests);
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
                .build(key, () -> configurationForLongRunningTests);
        int durationSeconds = System.getenv("CI") == null ? 5 : 1;
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(durationSeconds), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @MethodSource("asyncSpecs")
    @ParameterizedTest
    public <K, P extends AsyncProxyManager<K>, B extends AbstractAsyncProxyManagerBuilder<K, P, B>> void testTryConsumeAsync(AsyncProxyManagerSpec<K, P, B> spec) throws Exception {
        if (spec == NOPE) {
            return;
        }
        AsyncProxyManager<K> proxyManager = spec.builder.get().build();
        K key = spec.generateRandomKey();

        Function<AsyncBucketProxy, Long> action = bucket -> {
            try {
                return bucket.tryConsume(1).get() ? 1L : 0L;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        };
        Supplier<AsyncBucketProxy> bucketSupplier = () -> proxyManager.builder()
                .build(key, () -> CompletableFuture.completedFuture(configurationForLongRunningTests));
        int durationSeconds = System.getenv("CI") == null ? 5 : 1;
        AsyncConsumptionScenario scenario = new AsyncConsumptionScenario(4, TimeUnit.SECONDS.toNanos(durationSeconds), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @MethodSource("asyncSpecs")
    @ParameterizedTest
    public <K, P extends AsyncProxyManager<K>, B extends AbstractAsyncProxyManagerBuilder<K, P, B>> void testTryConsumeAsyncWithLimit(AsyncProxyManagerSpec<K, P, B> spec) throws Exception {
        if (spec == NOPE) {
            return;
        }
        AsyncProxyManager<K> proxyManager = spec.builder.get().build();

        K key = spec.generateRandomKey();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Function<AsyncBucketProxy, Long> action = bucket -> {
            try {
                return bucket.asScheduler().tryConsume(1, TimeUnit.MILLISECONDS.toNanos(50), scheduler).get() ? 1L :0L;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        };
        Supplier<AsyncBucketProxy> bucketSupplier = () -> proxyManager.builder()
                .build(key, () -> CompletableFuture.completedFuture(configurationForLongRunningTests));
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
                .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofDays(1)).build())
                .build();

        Bucket bucket1 = proxyManager.builder().build(key, () -> configuration);
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
                .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofDays(1)).build())
                .build();

        Bucket bucket = proxyManager.builder().build(key, () -> configuration);
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
            .addLimit(limit -> limit.capacity(MIN_CAPACITY).refillIntervally(4, Duration.ofMinutes(20)))
            .addLimit(limit -> limit.capacity(MAX_CAPACITY).refillIntervally(10, Duration.ofMinutes(60)))
            .build();

        ProxyManager<K> proxyManager = spec.builder.get().build();
        K key = spec.generateRandomKey();
        Bucket bucket = proxyManager.builder().build(key, () -> configuration);

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
                .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofDays(1)).build())
                .build();

        ProxyManager<K> proxyManager = spec.builder.get().build();
        ProxyManager<String> mappedProxyManager = proxyManager.withMapper(dummy -> key);
        Bucket unmappedBucket = proxyManager.builder().build(key, () -> configuration);
        Bucket mappedBucket = mappedProxyManager.builder().build("dummy", () -> configuration);
        Bucket mappedBucket2 = mappedProxyManager.builder().build("dummy2", () -> configuration);

        assertTrue(unmappedBucket.tryConsume(10));
        assertFalse(mappedBucket.tryConsume(1));
        assertFalse(mappedBucket2.tryConsume(1));

        unmappedBucket.reset();

        assertTrue(mappedBucket.tryConsume(5));
        assertTrue(mappedBucket2.tryConsume(5));
        assertFalse(unmappedBucket.tryConsume(1));
    }

    @MethodSource("asyncSpecs")
    @ParameterizedTest
    public <K, P extends AsyncProxyManager<K>, B extends AbstractAsyncProxyManagerBuilder<K, P, B>> void testWithMapperAsync(AsyncProxyManagerSpec<K, P, B> spec) throws Exception {
        if (spec == NOPE) {
            return;
        }
        AsyncProxyManager<K> proxyManager = spec.builder.get().build();

        K key = spec.generateRandomKey();
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofDays(1)).build())
                .build();

        AsyncProxyManager<String> mappedProxyManager = proxyManager.withMapper(dummy -> key);
        AsyncBucketProxy unmappedBucket = proxyManager.builder().build(key, () -> CompletableFuture.completedFuture(configuration));
        AsyncBucketProxy mappedBucket = mappedProxyManager.builder().build("dummy", () -> CompletableFuture.completedFuture(configuration));
        AsyncBucketProxy mappedBucket2 = mappedProxyManager.builder().build("dummy2", () -> CompletableFuture.completedFuture(configuration));

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

    @MethodSource("asyncSpecs")
    @ParameterizedTest
    public <K, P extends AsyncProxyManager<K>, B extends AbstractAsyncProxyManagerBuilder<K, P, B>> void test_1000_tokens_consumption_async(AsyncProxyManagerSpec<K, P, B> spec) throws InterruptedException, ExecutionException {
        if (spec == NOPE) {
            return;
        }
        AsyncProxyManager<K> proxyManager = spec.builder.get().build();

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
            AsyncBucketProxy bucket = proxyManager.builder().build(key, () -> CompletableFuture.completedFuture(configuration));
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

        long availableTokens = proxyManager.builder().build(key, () -> CompletableFuture.completedFuture(configuration)).getAvailableTokens().get();
        System.out.println("availableTokens " + availableTokens);
        System.out.println("Failed threads " + errors.keySet());
        System.out.println("Updates by thread " + updatesByThread);
        assertTrue(errors.isEmpty());
        assertEquals(capacity - opsCount, availableTokens);
    }

}
