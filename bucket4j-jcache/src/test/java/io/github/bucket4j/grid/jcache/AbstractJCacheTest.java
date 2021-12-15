package io.github.bucket4j.grid.jcache;


import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.Test;

import javax.cache.Cache;
import java.util.UUID;

public abstract class AbstractJCacheTest extends AbstractDistributedBucketTest<String> {

    @Test(expected = UnsupportedOperationException.class)
    public void testThatAsyncNotSupported() {
        getProxyManager().asAsync();
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
