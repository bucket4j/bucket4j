package io.github.bucket4j.redis;

import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;

import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;

public class LettuceBasedProxyManagerSentinelTest extends AbstractDistributedBucketTest {

    private static GenericContainer redisContainer;
    private static GenericContainer sentinelContainer;
    private static RedisClient redisClient;
    private static StatefulRedisMasterReplicaConnection<String, byte[]> redisConnection;

    @BeforeAll
    public static void setup() {

        redisContainer = new GenericContainer("redis:4.0.11")
                .withExposedPorts(6379);
        redisContainer.start();

        Integer redisPort = redisContainer.getMappedPort(6379);

        sentinelContainer = new GenericContainer("redis:4.0.11")
                .withExposedPorts(26379)
                .withCommand(
                        "sh", "-c",
                        "echo 'port 26379\nsentinel monitor mymaster 127.0.0.1 " + redisPort + " 1\nsentinel down-after-milliseconds mymaster 3000' > /tmp/sentinel.conf && redis-server /tmp/sentinel.conf --sentinel"
                );
        sentinelContainer.start();

        Integer sentinelPort = sentinelContainer.getMappedPort(26379);

        // 3. Setup the Sentinel URI directly against the IPv4 address
        RedisURI sentinelUri = RedisURI.builder()
                .withSentinelMasterId("mymaster")
                .withSentinel("127.0.0.1", sentinelPort)
                .build();

        redisClient = RedisClient.create(sentinelUri);
        RedisCodec<String, byte[]> bucket4jCodec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);

        // Establish the MasterReplica (topology aware connection context exactly once)
        redisConnection = MasterReplica.connect(redisClient, bucket4jCodec, sentinelUri);

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 5000) {
            try {
                // Perform a lightweight ping over the topology interface to check if Master is resolved
                redisConnection.sync().ping();
                break; // Connection is fully topology-aware
            } catch (Exception e) {
                try {
                    Thread.sleep(200); //allow some time for the thread to wait until leader election
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        specs = Arrays.asList(
                new ProxyManagerSpec<>(
                        "LettuceBasedProxyManager_Sentinel_StringKey",
                        () -> UUID.randomUUID().toString(),
                        () -> Bucket4jLettuce.casBasedBuilder(redisConnection)
                ).checkExpiration()
        );
    }

    @AfterAll
    public static void shutdown() {
        try {
            if (redisConnection != null) {
                redisConnection.close();
            }
        } finally {
            try {
                if (redisClient != null) {
                    redisClient.shutdown();
                }
            } finally {
                try {
                    if (sentinelContainer != null) {
                        sentinelContainer.close();
                    }
                } finally {
                    if (redisContainer != null) {
                        redisContainer.close();
                    }
                }
            }
        }
    }
}