package io.github.bucket4j.redis;

import glide.api.BaseClient;
import glide.api.GlideClient;
import glide.api.models.GlideString;
import glide.api.models.configuration.GlideClientConfiguration;
import glide.api.models.configuration.NodeAddress;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.glide.Bucket4jGlide;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.BackwardCompatibilityStateCheckHelper;
import io.github.bucket4j.tck.ProxyManagerSpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Runs the same TCK suite as {@link GlideBasedProxyManagerStandaloneTest} but against a
 * real Valkey server instead of Redis, to verify the claim in
 * <a href="https://github.com/bucket4j/bucket4j/issues/520">#520</a> that Bucket4j's Glide
 * integration is Valkey-compatible.
 */
public class GlideBasedProxyManagerValkeyStandaloneTest extends AbstractDistributedBucketTest {

    private static GenericContainer container;

    private static BaseClient client;

    @BeforeAll
    public static void setup() {
        container = startValkeyContainer();

        client = createGlideClient(container);

        BackwardCompatibilityStateCheckHelper<byte[]> byteArrayKeyHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(byte[] key) {
                try {
                    GlideString value = client.get(GlideString.of(key)).get();
                    return value == null ? null : value.getBytes();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void setRawState(byte[] key, byte[] state) {
                try {
                    client.set(GlideString.of(key), GlideString.of(state)).get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        BackwardCompatibilityStateCheckHelper<String> stringKeyHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(String key) {
                return byteArrayKeyHelper.getRawState(key.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public void setRawState(String key, byte[] state) {
                byteArrayKeyHelper.setRawState(key.getBytes(StandardCharsets.UTF_8), state);
            }
        };

        specs = Arrays.asList(
                new ProxyManagerSpec<>(
                        "GlideBasedProxyManager_ByteArrayKey",
                        () -> UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8),
                        () -> Bucket4jGlide.casBasedBuilder(client)
                ).checkExpiration().checkStateBackwardCompatibility(byteArrayKeyHelper).withoutBackwardCompatibilityRequestChecker(),
                new ProxyManagerSpec<>(
                        "GlideBasedProxyManager_StringKey",
                        () -> UUID.randomUUID().toString(),
                        () -> Bucket4jGlide.casBasedBuilder(client).keyMapper(Mapper.STRING)
                ).checkExpiration().checkStateBackwardCompatibility(stringKeyHelper).withoutBackwardCompatibilityRequestChecker()
        );
    }

    @AfterAll
    public static void shutdown() {
        try {
            if (client != null) {
                client.close();
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            if (container != null) {
                container.close();
            }
        }
    }

    private static GenericContainer startValkeyContainer() {
        GenericContainer genericContainer = new GenericContainer("valkey/valkey:8.0")
                .withExposedPorts(6379);
        genericContainer.start();
        return genericContainer;
    }

    private static BaseClient createGlideClient(GenericContainer container) {
        try {
            return GlideClient.createClient(GlideClientConfiguration.builder()
                    .lazyConnect(true)
                    .address(NodeAddress.builder()
                            .host(container.getHost())
                            .port(container.getMappedPort(6379))
                            .build())
                    .build()).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
