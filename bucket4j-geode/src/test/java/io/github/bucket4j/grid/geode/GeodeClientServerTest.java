package io.github.bucket4j.grid.geode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;
import org.slf4j.LoggerFactory;

/**
 * Runs the full Bucket4j TCK - including its concurrency tests - against both {@link GeodeProxyManager}
 * and {@link GeodeFunctionProxyManager}, wired up to a real, out-of-process Geode peer member (started in
 * a Docker container by {@link GeodeServerMain}), through a genuine network {@link ClientCache}. Since
 * {@link Bucket4jGeode#functionBasedBuilder(org.apache.geode.cache.Region)} ships the
 * {@link GeodeBucketFunction} instance to the server directly via {@code Execution.execute(Function)},
 * running it here also proves the server actually has the class on its classpath - the deployment cost
 * that {@link GeodeProxyManager} avoids.
 *
 * <p>Unlike {@link GeodeTest}, which only proves the CAS-transaction approach works for an embedded
 * peer-only topology, this test exercises the actual deployment topology real users have: a client
 * application connecting over the network to a remote Geode server. It exists to verify, empirically,
 * whether {@link org.apache.geode.cache.CacheTransactionManager} still detects concurrent modifications
 * once the region is only ever reached through a client proxy - see
 * {@link GeodeClientServerTransactionTest} for the minimal, isolated repro of that same question.
 */
public class GeodeClientServerTest extends AbstractDistributedBucketTest {

    private static final int CACHE_SERVER_PORT = 40404;
    private static final String REGION_NAME = "my_buckets";

    private static GenericContainer<?> serverContainer;
    private static ClientCache clientCache;

    @BeforeAll
    public static void setup() throws IOException {
        String classpath = Files.readString(Path.of("target/test-server-classpath.txt")).trim()
            + ":" + Path.of("target/classes").toAbsolutePath()
            + ":" + Path.of("target/test-classes").toAbsolutePath();

        String m2Repo = Path.of(System.getProperty("user.home"), ".m2", "repository").toAbsolutePath().toString();
        String repoRoot = Path.of("..").toAbsolutePath().normalize().toString();

        serverContainer = new GenericContainer<>(DockerImageName.parse("eclipse-temurin:17-jre-jammy"))
            .withFileSystemBind(m2Repo, m2Repo, BindMode.READ_ONLY)
            .withFileSystemBind(repoRoot, repoRoot, BindMode.READ_ONLY)
            .withExposedPorts(CACHE_SERVER_PORT)
            .withCommand("java", "-cp", classpath, "io.github.bucket4j.grid.geode.GeodeServerMain",
                String.valueOf(CACHE_SERVER_PORT), REGION_NAME)
            .waitingFor(Wait.forLogMessage(".*GEODE_SERVER_READY.*\\n", 1))
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(GeodeClientServerTest.class)));
        serverContainer.start();

        clientCache = new ClientCacheFactory()
            .addPoolServer(serverContainer.getHost(), serverContainer.getMappedPort(CACHE_SERVER_PORT))
            .set("log-level", "warning")
            .create();
        org.apache.geode.cache.Region<String, byte[]> clientRegion = clientCache
            .<String, byte[]>createClientRegionFactory(ClientRegionShortcut.PROXY)
            .create(REGION_NAME);

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "GeodeProxyManager_ClientServer",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jGeode.compareAndSwapBasedBuilder(clientRegion)
            ),
            new ProxyManagerSpec<>(
                "GeodeFunctionProxyManager_ClientServer",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jGeode.functionBasedBuilder(clientRegion)
            )
        );
    }

    @AfterAll
    public static void shutdown() {
        try {
            if (clientCache != null) {
                clientCache.close();
            }
        } finally {
            if (serverContainer != null) {
                serverContainer.close();
            }
        }
    }

}
