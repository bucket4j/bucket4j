package io.github.bucket4j.redis;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;

import java.util.Objects;

public class AbstractRedisProxyManagerBuilder<T extends AbstractRedisProxyManagerBuilder> {

    private ExpirationAfterWriteStrategy expirationStrategy;
    private ClientSideConfig clientSideConfig = ClientSideConfig.getDefault();

    public T withExpirationStrategy(ExpirationAfterWriteStrategy expirationStrategy) {
        this.expirationStrategy = Objects.requireNonNull(expirationStrategy);
        return (T) this;
    }

    public T withClientSideConfig(ClientSideConfig clientSideConfig) {
        this.clientSideConfig = Objects.requireNonNull(clientSideConfig);
        return (T) this;
    }

    public ExpirationAfterWriteStrategy getNotNullExpirationStrategy() {
        if (expirationStrategy == null) {
            throw new IllegalStateException("expirationStrategy is not configured");
        }
        return expirationStrategy;
    }

    public ClientSideConfig getClientSideConfig() {
        return clientSideConfig;
    }

}
