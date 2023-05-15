package io.github.bucket4j.grid.jcache;


import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.junit.jupiter.api.Test;

import javax.cache.Cache;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class AbstractJCacheTest extends AbstractDistributedBucketTest<String> {

    @Test
    public void testThatAsyncNotSupported() {
        assertThrows(UnsupportedOperationException.class, () -> {
            getProxyManager().asAsync();
        });
    }

    @Override
    protected ProxyManager<String> getProxyManager() {
        return new JCacheProxyManager<>(getCache(), ClientSideConfig.getDefault());
    }

    @Override
    protected String generateRandomKey() {
        return UUID.randomUUID().toString();
    }

    protected abstract Cache<String, byte[]> getCache();

}
