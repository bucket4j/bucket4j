package example.distributed.optimizers.manual_syncing;

import com.codahale.metrics.Snapshot;
import com.github.rollingmetrics.counter.SmoothlyDecayingRollingCounter;
import com.github.rollingmetrics.dropwizard.Dropwizard;
import com.github.rollingmetrics.histogram.OverflowResolver;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.manual.ManuallySyncingBucketSynchronization;
import io.github.bucket4j.grid.hazelcast.Bucket4jHazelcast;
import io.github.bucket4j.grid.hazelcast.HazelcastProxyManager;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class HazelcastWithManualSyningPerformanceExample {

    private static Logger logger = LoggerFactory.getLogger(HazelcastWithManualSyningPerformanceExample.class);

    private static IMap<String, byte[]> map;
    private static Cloud cloud;
    private static ViNode server;

    private static HazelcastInstance hazelcastInstance;

    @BeforeClass
    public static void setup() {
        // start separated JVM on current host
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
        server = cloud.node("stateful-hazelcast-server");

        server.exec((Runnable & Serializable) () -> {
            Config config = new Config();
            config.setLiteMember(false);
            HazelcastProxyManager.addCustomSerializers(config.getSerializationConfig(), 100);
            HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
            hazelcastInstance.getMap("my_buckets");
        });

        // start hazelcast client which works inside current JVM and does not hold data
        Config config = new Config();
        HazelcastProxyManager.addCustomSerializers(config.getSerializationConfig(), 100);
        config.setLiteMember(true);
        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        map = hazelcastInstance.getMap("my_buckets");
    }

    @AfterClass
    public static void shutdown() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
        if (cloud != null) {
            cloud.shutdown();
        }
    }


    @Test
    public void benchmarkSyncBucket() throws InterruptedException {
        SmoothlyDecayingRollingCounter consumptionRate = new SmoothlyDecayingRollingCounter(Duration.ofSeconds(10), 5);
        com.codahale.metrics.Timer latencyTimer = buildLatencyTimer();

        HazelcastProxyManager<String> proxyManager = Bucket4jHazelcast.entryProcessorBasedBuilder(map).build();
//        ProxyManagerMock<String> proxyManager = new ProxyManagerMock<>(TimeMeter.SYSTEM_MILLISECONDS);
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(10000).refillGreedy(10000, Duration.ofNanos(1)).initialTokens(0))
                .build();

        BucketProxy bucket = proxyManager.builder()
                .withOptimization(new ManuallySyncingBucketSynchronization())
                .build("13", () -> configuration);
        // Fetching available tokens is fully enough init bucket in storage
        bucket.getAvailableTokens();

        BucketProxy nonOptimizedBucket = proxyManager.builder()
                .build("13", () -> configuration);

        // sync bucket manually each seconds
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            bucket.getOptimizationController().syncImmediately();
            }, 1, 1, TimeUnit.SECONDS
        );

        AtomicLong consumedTokens = new AtomicLong();

        Timer statLogTimer = new Timer();
        statLogTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Consumption rate " + consumptionRate.getSum() / 10 + " tokens/sec");
                System.out.println("local tokens " + bucket.getAvailableTokens());
                System.out.println("remote tokens " + nonOptimizedBucket.getAvailableTokens());
                System.out.println("consumed tokens " + consumedTokens.get());
                System.out.println("Operations with bucket rate " + latencyTimer.getOneMinuteRate() + " ops/sec");
                Snapshot snapshot = latencyTimer.getSnapshot();
                System.out.println("Operations with bucket latency:" +
                        " mean=" + TimeUnit.NANOSECONDS.toMicros((long) snapshot.getMean()) + "micros" +
                        " median=" + TimeUnit.NANOSECONDS.toMicros((long)snapshot.getMedian()) + "micros" +
                        " max=" + TimeUnit.NANOSECONDS.toMillis(snapshot.getMax()) + "millis");

                System.out.println("---------------------------------------------");
            }
        }, 1_000, 1_000);

        int PARALLEL_THREADS = 100;
        // start
        for (int i = 0; i < PARALLEL_THREADS; i++) {
            Thread thread = new Thread(() -> {
                while (true) {
                    try (com.codahale.metrics.Timer.Context ctx = latencyTimer.time()) {
                        if (bucket.tryConsume(1)) {
                            consumptionRate.add(1);
                            consumedTokens.incrementAndGet();
                        }
                    } catch (Throwable t) {
                        logger.error("Failed to consume tokens from bucket", t);
                    }
                }
            });
            thread.setName("Bucket consumer " + i);
            thread.start();
        }

        Thread.currentThread().join();
    }

    @Test
    public void benchmarkAsyncBucket() throws InterruptedException {
        SmoothlyDecayingRollingCounter consumptionRate = new SmoothlyDecayingRollingCounter(Duration.ofSeconds(10), 5);
        com.codahale.metrics.Timer latencyTimer = buildLatencyTimer();

        HazelcastProxyManager<String> proxyManager = Bucket4jHazelcast.entryProcessorBasedBuilder(map).build();;
//        ProxyManagerMock<String> proxyManager = new ProxyManagerMock<>(TimeMeter.SYSTEM_MILLISECONDS);
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(
                        Bandwidth.builder().capacity(10000).refillGreedy(10000, Duration.ofSeconds(1)).initialTokens(0).build())
                .build();

        AsyncBucketProxy bucket = proxyManager.asAsync().builder()
                .withOptimization(new ManuallySyncingBucketSynchronization())
                .build("13", () -> CompletableFuture.completedFuture(configuration));
        bucket.getAvailableTokens().join();

        BucketProxy nonOptimizedBucket = proxyManager.builder()
                .build("13", () -> configuration);

        // sync bucket manually each second
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
                    bucket.getOptimizationController().syncImmediately();
                }, 1, 1, TimeUnit.SECONDS
        );

        AtomicLong consumedTokens = new AtomicLong();

        // We need a backpressure for ougoing work because it obviously that OOM can be happen in asycnhrouous bucket mode
        // when tasks incoming rate is greater then Hazelcast can process
        Semaphore semaphore = new Semaphore(20_00); // no more then 20_000 throttling requests in progress

        Timer statLogTimer = new Timer();
        statLogTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Consumption rate " + consumptionRate.getSum() / 10 + " tokens/sec");
                try {
                    System.out.println("local tokens " + bucket.getAvailableTokens().get());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("remote tokens " + nonOptimizedBucket.getAvailableTokens());
                System.out.println("consumed tokens " + consumedTokens.get());
                System.out.println("Operations with bucket rate " + latencyTimer.getOneMinuteRate() + " ops/sec");
                Snapshot snapshot = latencyTimer.getSnapshot();
                System.out.println(
                        "Operations with bucket latency:" +
                        " mean=" + TimeUnit.NANOSECONDS.toMicros((long) snapshot.getMean()) + "micros" +
                        " median=" + TimeUnit.NANOSECONDS.toMicros((long)snapshot.getMedian()) + "micros" +
                        " max=" + TimeUnit.NANOSECONDS.toMillis(snapshot.getMax()) + "millis" +
                        " current_requests_in_progress=" + semaphore.availablePermits()
                );
                System.out.println("---------------------------------------------");
            }
        }, 1_000, 1_000);

        int PARALLEL_THREADS = 4;
        // start
        for (int i = 0; i < PARALLEL_THREADS; i++) {
            Thread thread = new Thread(() -> {
                while (true) {
                    try {
                        semaphore.acquire();
                        long currentTimeNanos = System.nanoTime();
                        bucket.tryConsume(1).whenComplete((consumed, error) -> {
                            long latencyNanos = System.nanoTime() - currentTimeNanos;
                            semaphore.release();
                            latencyTimer.update(latencyNanos, TimeUnit.NANOSECONDS);
                            if (error != null) {
                               logger.error("Failed to consume tokens from bucket", error);
                               return;
                            }
                            if (consumed) {
                               consumptionRate.add(1);
                                consumedTokens.incrementAndGet();
                            }
                        });
                    } catch (Throwable t) {
                        semaphore.release();
                        logger.error("Failed to consume tokens from bucket", t);
                    }
                }
            });
            thread.setName("Bucket consumer " + i);
            thread.start();
        }

        Thread.currentThread().join();
    }

    private com.codahale.metrics.Timer buildLatencyTimer() {
        RollingHdrHistogram histogram = RollingHdrHistogram.builder()
                .withSignificantDigits(2)
                .resetReservoirPeriodicallyByChunks(Duration.ofSeconds(60), 3)
                .withHighestTrackableValue(1_000_000_000_000L, OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .build();
        return Dropwizard.toTimer(histogram);
    }

}
