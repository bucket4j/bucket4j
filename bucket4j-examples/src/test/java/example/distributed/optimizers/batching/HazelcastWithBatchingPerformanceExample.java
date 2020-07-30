package example.distributed.optimizers.batching;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Snapshot;
import com.github.rollingmetrics.dropwizard.Dropwizard;
import com.github.rollingmetrics.histogram.OverflowResolver;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.hazelcast.map.IMap;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.proxy.optimization.Optimizations;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.github.bucket4j.grid.hazelcast.HazelcastBackend;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class HazelcastWithBatchingPerformanceExample {

    private static Logger logger = LoggerFactory.getLogger(HazelcastWithBatchingPerformanceExample.class);

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
            HazelcastBackend.addCustomSerializers(config.getSerializationConfig(), 100);
            HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
            hazelcastInstance.getMap("my_buckets");
        });

        // start hazelcast client which works inside current JVM and does not hold data
        Config config = new Config();
        HazelcastBackend.addCustomSerializers(config.getSerializationConfig(), 100);
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
        Meter consumptionRate = new Meter();
        com.codahale.metrics.Timer latencyTimer = buildLatencyTimer();

        HazelcastBackend<String> backend = new HazelcastBackend<>(map);
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(
                    Bandwidth.simple(10, Duration.ofSeconds(1)).withInitialTokens(0))
                .build();

        Bucket bucket = backend.builder()
                .withRequestOptimizer(Optimizations.batching())
                .buildProxy("13", configuration);

        Timer statLogTimer = new Timer();
        statLogTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Consumption rate " + consumptionRate.getOneMinuteRate() + " tokens/sec");
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
                            consumptionRate.mark();
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
        Meter consumptionRate = new Meter();
        com.codahale.metrics.Timer latencyTimer = buildLatencyTimer();

        HazelcastBackend<String> backend = new HazelcastBackend<>(map);
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(
                        Bandwidth.simple(10, Duration.ofSeconds(1)).withInitialTokens(0))
                .build();

        AsyncBucketProxy bucket = backend.asAsync().builder()
                .withRequestOptimizer(Optimizations.batching())
                .buildProxy("13", configuration);

        // We need a backpressure for ougoing work because it obviously that OOM can be happen in asycnhrouous bucket mode
        // when tasks incoming rate is greater then Hazelcast can process
        Semaphore semaphore = new Semaphore(20_00); // no more then 20_000 throttling requests in progress

        Timer statLogTimer = new Timer();
        statLogTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Consumption rate " + consumptionRate.getOneMinuteRate() + " tokens/sec");
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
                               consumptionRate.mark();
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
