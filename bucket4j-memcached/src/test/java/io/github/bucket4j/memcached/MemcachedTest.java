package io.github.bucket4j.memcached;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.spotify.folsom.ConnectFuture;
import com.spotify.folsom.MemcacheClient;
import com.spotify.folsom.MemcacheClientBuilder;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;

import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;

public class MemcachedTest extends AbstractDistributedBucketTest {

    private static GenericContainer<?> container;
    private static MemcacheClient<byte[]> client;

    @BeforeAll
    public static void setup() throws IOException, InterruptedException, ExecutionException {
        container = new GenericContainer<>("memcached:1.6-alpine")
            .withExposedPorts(11211);
        container.start();

        client = MemcacheClientBuilder.newByteArrayClient()
            .withAddress(container.getHost(), container.getMappedPort(11211))
            .withConnectionTimeoutMillis(10_000)
            .connectAscii();
        ConnectFuture.connectFuture(client).toCompletableFuture().get();

        specs = List.of(
            new ProxyManagerSpec<>(
                "MemcachedCompareAndSwapBasedProxyManager",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jMemcached.casBasedBuilder(client)
            ).checkExpiration()
        );
    }

    @AfterAll
    public static void shutdown() {
        if (client != null) {
            client.shutdown();
        }
        if (container != null) {
            container.stop();
        }
    }

}

