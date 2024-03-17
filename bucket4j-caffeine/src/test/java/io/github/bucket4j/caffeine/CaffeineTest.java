package io.github.bucket4j.caffeine;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;

public class CaffeineTest extends AbstractDistributedBucketTest {

    @BeforeAll
    public static void initParams() {
        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "CaffeineProxyManager",
                () -> UUID.randomUUID().toString(),
                clientConfig -> new CaffeineProxyManager<>(Caffeine.newBuilder().maximumSize(100), Duration.ofMinutes(1), clientConfig)
            )
        );
    }

}
