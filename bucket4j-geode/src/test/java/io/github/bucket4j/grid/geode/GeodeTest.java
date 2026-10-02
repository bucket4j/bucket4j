package io.github.bucket4j.grid.geode;

import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.BackwardCompatibilityRequestCheckHelper;
import io.github.bucket4j.tck.BackwardCompatibilityStateCheckHelper;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.FunctionService;
import org.apache.geode.cache.execute.ResultCollector;
import org.apache.geode.cache.RegionShortcut;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.github.bucket4j.distributed.proxy.ExecutionStrategy.background;

/**
 * Runs the Bucket4j TCK against an embedded, in-process Geode peer member.
 *
 * <p>Geode enforces a single {@code GemFireCache} instance per JVM, and a peer {@code Cache} cannot
 * coexist with a {@code ClientCache} in the same process - exercising a real client/server topology
 * would require forking a second JVM (the way {@code bucket4j-ignite} does for its thick-client test).
 * Since {@link GeodeProxyManager} operates uniformly on any {@link Region}, running directly against
 * an embedded peer region still exercises the real cache-transaction-based command execution path.
 */
public class GeodeTest extends AbstractDistributedBucketTest {

    private static Cache cache;
    private static ExecutorService backgroundExecutor;

    @BeforeAll
    public static void setup() {
        cache = new CacheFactory()
            .set("mcast-port", "0")
            .set("locators", "")
            .set("log-level", "warning")
            .create();
        Region<String, byte[]> region = cache.<String, byte[]>createRegionFactory(RegionShortcut.PARTITION).create("my_buckets");
        backgroundExecutor = Executors.newFixedThreadPool(20);

        BackwardCompatibilityStateCheckHelper<String> backwardCompatibilityStateCheckHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(String key) {
                return region.get(key);
            }
            @Override
            public void setRawState(String key, byte[] state) {
                region.put(key, state);
            }
        };

        BackwardCompatibilityRequestCheckHelper<String> requestCompatibilityHelper = new BackwardCompatibilityRequestCheckHelper<>() {
            @Override
            public byte[] execute(String key, byte[] requestBytes) {
                Execution<byte[], byte[], List<byte[]>> execution = FunctionService.<byte[], byte[], List<byte[]>>onRegion(region)
                    .withFilter(Collections.singleton(key))
                    .setArguments(requestBytes);
                ResultCollector<byte[], List<byte[]>> resultCollector = execution.execute(new GeodeBucketFunction<String>());
                return resultCollector.getResult().get(0);
            }

            @Override
            public CompletableFuture<byte[]> executeAsync(String key, byte[] requestBytes) {
                throw new UnsupportedOperationException();
            }
        };

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "GeodeProxyManager",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jGeode.compareAndSwapBasedBuilder(region)
            ).checkStateBackwardCompatibility(backwardCompatibilityStateCheckHelper).withoutBackwardCompatibilityRequestChecker(),
            new ProxyManagerSpec<>(
                "GeodeProxyManager_background",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jGeode.compareAndSwapBasedBuilder(region)
                    .executionStrategy(background(backgroundExecutor))
            ).checkStateBackwardCompatibility(backwardCompatibilityStateCheckHelper).withoutBackwardCompatibilityRequestChecker(),
            new ProxyManagerSpec<>(
                "GeodeFunctionProxyManager",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jGeode.functionBasedBuilder(region)
            ).checkStateBackwardCompatibility(backwardCompatibilityStateCheckHelper).checkRequestBackwardCompatibility(requestCompatibilityHelper)
        );
    }

    @AfterAll
    public static void shutdown() {
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdown();
        }
        if (cache != null) {
            cache.close();
        }
    }

}
