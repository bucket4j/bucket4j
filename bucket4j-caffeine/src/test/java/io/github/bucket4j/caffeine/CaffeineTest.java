package io.github.bucket4j.caffeine;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;

import java.time.Duration;
import java.util.UUID;

public class CaffeineTest extends AbstractDistributedBucketTest<String> {

    @Override
    protected ProxyManager<String> getProxyManager() {
        Caffeine<String, RemoteBucketState> builder = (Caffeine) Caffeine.newBuilder().maximumSize(100);
        return new CaffeineProxyManager<>(builder, Duration.ofMinutes(1));
    }

    @Override
    protected String generateRandomKey() {
        return UUID.randomUUID().toString();
    }

}
