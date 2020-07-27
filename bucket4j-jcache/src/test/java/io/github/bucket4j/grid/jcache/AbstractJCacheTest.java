package io.github.bucket4j.grid.jcache;


import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.Backend;
import org.junit.Test;

import javax.cache.Cache;
import java.time.Duration;
import java.util.UUID;

public abstract class AbstractJCacheTest extends AbstractDistributedBucketTest {

    @Test(expected = UnsupportedOperationException.class)
    public void testThatAsyncNotSupported() {
        getBackend().asAsync();
    }

    @Override
    protected Backend<String> getBackend() {
        return new JCacheBackend<>(getCache());
    }

    @Override
    protected void removeBucketFromBackingStorage(String key) {
        getCache().remove(key);
    }

    protected abstract Cache<String, byte[]> getCache();

}
