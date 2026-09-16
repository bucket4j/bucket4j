package io.github.bucket4j.grid.geode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.geode.cache.CacheTransactionManager;
import org.apache.geode.cache.CommitConflictException;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Runs a real, out-of-process Geode peer member (in a Docker container, started via
 * {@link GeodeServerMain}) and connects to it with a genuine {@link ClientCache}, in order to verify
 * whether {@link CacheTransactionManager} actually detects concurrent modifications for a region that
 * is only ever accessed through a client/server topology (as opposed to the embedded, single-process
 * peer-only topology that {@link GeodeTest} exercises).
 *
 * <p>This test exists because the official {@code CacheTransactionManager} javadoc states that "client
 * Regions ... do not support transactions", which would mean {@link GeodeProxyManager}'s compare-and-swap
 * loop - which relies on {@link CommitConflictException} to detect lost races - silently loses updates
 * whenever it runs against a real client/server deployment instead of an embedded peer member.
 */
public class GeodeClientServerTransactionTest {

    private static final int CACHE_SERVER_PORT = 40404;
    private static final String REGION_NAME = "my_buckets";

    private static GenericContainer<?> serverContainer;
    private static ClientCache clientCache;
    private static Region<String, byte[]> clientRegion;

    @BeforeAll
    public static void setup() throws IOException {
        String classpath = Files.readString(Path.of("target/test-server-classpath.txt")).trim()
            + ":" + Path.of("target/classes").toAbsolutePath()
            + ":" + Path.of("target/test-classes").toAbsolutePath();

        String m2Repo = Path.of(System.getProperty("user.home"), ".m2", "repository").toAbsolutePath().toString();
        String projectTarget = Path.of("target").toAbsolutePath().toString();

        serverContainer = new GenericContainer<>(DockerImageName.parse("eclipse-temurin:17-jre-jammy"))
            .withFileSystemBind(m2Repo, m2Repo, org.testcontainers.containers.BindMode.READ_ONLY)
            .withFileSystemBind(projectTarget, projectTarget, org.testcontainers.containers.BindMode.READ_ONLY)
            .withExposedPorts(CACHE_SERVER_PORT)
            .withCommand("java", "-cp", classpath, "io.github.bucket4j.grid.geode.GeodeServerMain",
                String.valueOf(CACHE_SERVER_PORT), REGION_NAME)
            .waitingFor(Wait.forLogMessage(".*GEODE_SERVER_READY.*\\n", 1))
            .withLogConsumer(new Slf4jLogConsumer(org.slf4j.LoggerFactory.getLogger(GeodeClientServerTransactionTest.class)));
        serverContainer.start();

        clientCache = new ClientCacheFactory()
            .addPoolServer(serverContainer.getHost(), serverContainer.getMappedPort(CACHE_SERVER_PORT))
            .set("log-level", "warning")
            .create();
        clientRegion = clientCache.<String, byte[]>createClientRegionFactory(ClientRegionShortcut.PROXY)
            .create(REGION_NAME);
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

    /**
     * Two racers read the same key inside their own transaction, then both try to overwrite it and commit.
     * If Geode actually enforces per-key conflict detection for a client-region transaction (the same
     * guarantee {@link GeodeProxyManager} relies on), exactly one of the two commits must fail with
     * {@link CommitConflictException}. If both commits succeed, the update from whichever racer commits
     * first is silently lost - which is exactly the failure mode the Bucket4j TCK's concurrency tests are
     * designed to catch, and would mean the current CAS-transaction-based approach does not work at all
     * against a real client/server deployment.
     */
    @Test
    public void clientRegionTransactionDetectsConcurrentModification() throws Exception {
        String key = "race-key";
        clientRegion.put(key, new byte[] { 0 });

        CacheTransactionManager txManager = clientCache.getCacheTransactionManager();
        CountDownLatch bothRead = new CountDownLatch(2);

        Callable<Void> racer = () -> {
            txManager.begin();
            byte[] current = clientRegion.get(key);
            bothRead.countDown();
            bothRead.await(10, TimeUnit.SECONDS);
            clientRegion.put(key, new byte[] { (byte) (current[0] + 1) });
            txManager.commit();
            return null;
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Void> f1 = pool.submit(racer);
            Future<Void> f2 = pool.submit(racer);

            int conflicts = 0;
            for (Future<Void> f : List.of(f1, f2)) {
                try {
                    f.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof CommitConflictException) {
                        conflicts++;
                    } else {
                        throw e;
                    }
                }
            }

            assertEquals(1, conflicts, "Expected exactly one of the two racing transactions to fail with "
                + "CommitConflictException. If this assertion fails with 0 conflicts, Geode client-region "
                + "transactions do not detect concurrent modifications, and GeodeProxyManager's CAS loop "
                + "silently loses updates against a real client/server deployment.");
        } finally {
            pool.shutdown();
        }
    }

}
