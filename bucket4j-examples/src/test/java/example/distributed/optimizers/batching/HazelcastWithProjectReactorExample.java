package example.distributed.optimizers.batching;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Snapshot;
import com.github.rollingmetrics.dropwizard.Dropwizard;
import com.github.rollingmetrics.histogram.OverflowResolver;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.SchedulingBucket;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.optimization.Optimizations;
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
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.time.Duration;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class HazelcastWithProjectReactorExample {

    private static Logger logger = LoggerFactory.getLogger(HazelcastWithProjectReactorExample.class);

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

    public static <T> Mono<T> decorate(SchedulingBucket bucket, ScheduledExecutorService scheduledExecutorService, int tokenToConsume, Supplier<Mono<T>> monoSupplier) {
        // if there is enough tokens in the bucket then returned future is immoderately completed
        // otherwise it will be completed inside some thread belonged to scheduledExecutorService after refilling requested amount of tokens
        CompletableFuture<Void> rateLimitingFuture = bucket.consume(tokenToConsume, scheduledExecutorService);
        CompletableFuture<T> resultFuture = rateLimitingFuture.thenCompose((Void nothing) -> {
            Mono<T> mono = monoSupplier.get();
            return mono.toFuture();
        });
        return Mono.fromFuture(resultFuture);
    }

    // this variant looks better for me than unlimited "decorate" because allows to specify maximum time that you are agree to wait for tokens refill
    public static <T> Mono<T> decorateWithWaitingLimit(SchedulingBucket bucket, Duration waitingLimit, ScheduledExecutorService scheduledExecutorService, int tokenToConsume, Supplier<Mono<T>> monoSupplier) {
        // if there is enough tokens in the bucket then returned future is immoderately completed
        // otherwise it will be completed inside some thread belonged to scheduledExecutorService after refilling requested amount of tokens.
        //
        // If there are no requested tokens in the bucket and requested amount can not be refilled in waitingLimit then future immediately completed by {@code false}.
        CompletableFuture<Boolean> rateLimitingFuture = bucket.tryConsume(tokenToConsume, waitingLimit, scheduledExecutorService);
        CompletableFuture<T> resultFuture = rateLimitingFuture.thenCompose((Boolean wasConsumed) -> {
            if (!wasConsumed) {
                // TODO replace with another exaption type that is more suitable for your application
                Exception exception = new TimeoutException(tokenToConsume + " tokens can not be consumed in " + waitingLimit.toMillis() + " milliseconds");
                CompletableFuture<T> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(exception);
                return failedFuture;
            }
            Mono<T> mono = monoSupplier.get();
            return mono.toFuture();
        });

        return Mono.fromFuture(resultFuture);
    }

    @Test
    public void testReactorIntegrationWithAsyncBucket_case_when_execution_postponed_to_4_seconds() {
        HazelcastProxyManager<String> proxyManager = new HazelcastProxyManager<>(map, ClientSideConfig.getDefault());
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(
                        Bandwidth.simple(10, Duration.ofSeconds(1)))
                .build();

        AsyncBucketProxy bucket = proxyManager.asAsync().builder()
                .build("13", configuration);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        System.out.println("Before interaction time " + new Date());
        Mono<Integer> mono = decorate(bucket.asScheduler(), scheduler, 50, () -> Mono.just(42));
        mono.block();
        System.out.println("After interaction time " + new Date());
    }

    @Test
    public void testReactorIntegrationWithAsyncBucket_case_when_requested_token_available() {
        HazelcastProxyManager<String> proxyManager = new HazelcastProxyManager<>(map, ClientSideConfig.getDefault());
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(
                        Bandwidth.simple(10, Duration.ofSeconds(1)))
                .build();

        AsyncBucketProxy bucket = proxyManager.asAsync().builder()
                .build("13", configuration);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        System.out.println("Before interaction time " + new Date());
        Mono<Integer> mono = decorate(bucket.asScheduler(), scheduler, 8, () -> Mono.just(42));
        mono.block();
        System.out.println("After interaction time " + new Date());
    }

    @Test
    public void testReactorIntegrationWithAsyncBucket_case_withLimit_when_requested_token_available() {
        HazelcastProxyManager<String> proxyManager = new HazelcastProxyManager<>(map, ClientSideConfig.getDefault());
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(
                        Bandwidth.simple(10, Duration.ofSeconds(1)))
                .build();

        AsyncBucketProxy bucket = proxyManager.asAsync().builder()
                .build("13", configuration);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        System.out.println("Before interaction time " + new Date());
        Mono<Integer> mono = decorateWithWaitingLimit(bucket.asScheduler(), Duration.ofSeconds(60), scheduler, 5, () -> Mono.just(42));
        mono.block();
        System.out.println("After interaction time " + new Date());
    }

    @Test
    public void testReactorIntegrationWithAsyncBucket_case_withLimit_when_execution_postponed_to_4_seconds() {
        HazelcastProxyManager<String> proxyManager = new HazelcastProxyManager<>(map, ClientSideConfig.getDefault());
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(
                        Bandwidth.simple(10, Duration.ofSeconds(1)))
                .build();

        AsyncBucketProxy bucket = proxyManager.asAsync().builder()
                .build("13", configuration);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        System.out.println("Before interaction time " + new Date());
        Mono<Integer> mono = decorateWithWaitingLimit(bucket.asScheduler(), Duration.ofSeconds(60), scheduler, 50, () -> Mono.just(42));
        mono.block();
        System.out.println("After interaction time " + new Date());
    }

    @Test
    public void testReactorIntegrationWithAsyncBucket_case_withLimit_when_execution_rejected_because_tokens_can_not_be_refiling_withing_threshold() {
        HazelcastProxyManager<String> proxyManager = new HazelcastProxyManager<>(map, ClientSideConfig.getDefault());
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(
                        Bandwidth.simple(10, Duration.ofSeconds(1)))
                .build();

        AsyncBucketProxy bucket = proxyManager.asAsync().builder()
                .withOptimization(Optimizations.batching())
                .build("13", configuration);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        System.out.println("Before interaction time " + new Date());
        Mono<Integer> mono = decorateWithWaitingLimit(bucket.asScheduler(), Duration.ofSeconds(1), scheduler, 50, () -> Mono.just(42));
        mono.block();
        System.out.println("After interaction time " + new Date());
    }

}
