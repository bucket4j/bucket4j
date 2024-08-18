package example.distributed.optimizers.batching;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.google.common.base.Charsets;

import com.codahale.metrics.Snapshot;
import com.github.rollingmetrics.counter.SmoothlyDecayingRollingCounter;
import com.github.rollingmetrics.dropwizard.Dropwizard;
import com.github.rollingmetrics.histogram.OverflowResolver;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.DefaultOptimizationListener;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.Optimization;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.Optimizations;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;

public class RedisBatchingExample {

    private static Logger logger = LoggerFactory.getLogger(RedisBatchingExample.class);

    private static GenericContainer container;
    private static RedisClient redisClient;

    @BeforeClass
    public static void setup() {
        container = startRedisContainer();
        redisClient = createLettuceClient(container);
    }

    @AfterClass
    public static void shutdown() {
        if (redisClient != null) {
            redisClient.shutdown();
        }
        if (container != null) {
            container.close();
        }
    }

    private static RedisClient createLettuceClient(GenericContainer container) {
        String redisHost = container.getHost();
        Integer redisPort = container.getMappedPort(6379);
        String redisUrl = "redis://" + redisHost + ":" + redisPort;

        return RedisClient.create(redisUrl);
    }

    private static GenericContainer startRedisContainer() {
        GenericContainer genericContainer = new GenericContainer("redis:7.0.2").withExposedPorts(6379);
        genericContainer.start();
        return genericContainer;
    }

    @Test
    public void benchmarkSyncBucket() throws InterruptedException {
        int PARALLEL_THREADS = 10;
        int TOKENS_PER_SEC = 300;

        SmoothlyDecayingRollingCounter consumptionRatePerSecond = new SmoothlyDecayingRollingCounter(Duration.ofSeconds(1), 10);
        SmoothlyDecayingRollingCounter rejectionRatePerSecond = new SmoothlyDecayingRollingCounter(Duration.ofSeconds(1), 10);
        com.codahale.metrics.Timer latencyTimer = buildLatencyTimer();

        ProxyManager<String> proxyManager = LettuceBasedProxyManager.builderFor(redisClient)
            .build()
            .withMapper(str -> str.getBytes(Charsets.UTF_8));
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(TOKENS_PER_SEC).refillGreedy(TOKENS_PER_SEC, Duration.ofSeconds(1)))
            .build();

        AtomicLong totalMergedRequestCount = new AtomicLong();
        AtomicLong totalSkippedRequestCount = new AtomicLong();

        DefaultOptimizationListener optimizationListener = new DefaultOptimizationListener();
        Optimization optimization = Optimizations.batching()
            .withListener(optimizationListener);

        Bucket bucket = proxyManager.builder()
                .withOptimization(optimization)
                .build("13", configuration);

        Timer statLogTimer = new Timer();
        statLogTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Consumption rate " + consumptionRatePerSecond.getSum() + " tokens/sec ");
                System.out.println("Rejection rate " + rejectionRatePerSecond.getSum() + " tokens/sec ");
                long skippedRequestCountSnapshot = optimizationListener.getSkipCount();
                long mergedRequestCountSnapshot = optimizationListener.getMergeCount();
                System.out.println("Optimization stat: " +
                    "skipped=" + (skippedRequestCountSnapshot - totalSkippedRequestCount.get()) + " " +
                    "merged=" + (mergedRequestCountSnapshot - totalMergedRequestCount.get()));
                totalSkippedRequestCount.set(skippedRequestCountSnapshot);
                totalMergedRequestCount.set(mergedRequestCountSnapshot);
                Snapshot snapshot = latencyTimer.getSnapshot();
                System.out.println("Operations with bucket latency:" +
                        " mean=" + TimeUnit.NANOSECONDS.toMicros((long) snapshot.getMean()) + "micros" +
                        " median=" + TimeUnit.NANOSECONDS.toMicros((long)snapshot.getMedian()) + "micros" +
                        " max=" + TimeUnit.NANOSECONDS.toMillis(snapshot.getMax()) + "millis");
                System.out.println("---------------------------------------------");
            }
        }, 10_000, 1_000);

        // start
        for (int i = 0; i < PARALLEL_THREADS; i++) {
            Thread thread = new Thread(() -> {
                while (true) {
                    try (com.codahale.metrics.Timer.Context ctx = latencyTimer.time()) {
                        if (bucket.tryConsume(1)) {
                            consumptionRatePerSecond.add(1);
                        } else {
                            rejectionRatePerSecond.add(1);
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

    private com.codahale.metrics.Timer buildLatencyTimer() {
        RollingHdrHistogram histogram = RollingHdrHistogram.builder()
                .withSignificantDigits(2)
                .resetReservoirPeriodicallyByChunks(Duration.ofSeconds(60), 3)
                .withHighestTrackableValue(1_000_000_000_000L, OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .build();
        return Dropwizard.toTimer(histogram);
    }

}
