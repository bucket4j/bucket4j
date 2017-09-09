package io.github.bucket4j.grid.jcache;


import io.github.bucket4j.*;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.grid.RecoveryStrategy;
import org.junit.Test;

import javax.cache.Cache;
import java.time.Duration;
import java.util.UUID;


import static io.github.bucket4j.grid.RecoveryStrategy.RECONSTRUCT;

public abstract class AbstractJCacheTest extends AbstractDistributedBucketTest<JCacheBucketBuilder, JCache> {

    @Test(expected = UnsupportedOperationException.class)
    public void testThatAsyncNotSupported() {
        Bucket bucket = Bucket4j.extension(JCache.class).builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build(getCache(), UUID.randomUUID().toString(), RECONSTRUCT);

        bucket.asAsync();
    }

    @Test
    @Override
    public void testThatImpossibleToPassNullCacheToProxyManagerConstructor() {
        Bucket4j.extension(JCache.class).proxyManagerForCache(null);
    }

    @Override
    protected Class<JCache> getExtensionClass() {
        return JCache.class;
    }

    @Override
    protected Bucket build(JCacheBucketBuilder builder, String key, RecoveryStrategy recoveryStrategy) {
        return builder.build(getCache(), key, recoveryStrategy);
    }

    @Override
    protected ProxyManager<String> newProxyManager() {
        return Bucket4j.extension(JCache.class).proxyManagerForCache(getCache());
    }

    @Override
    protected void removeBucketFromBackingStorage(String key) {
        getCache().remove(key);
    }

    protected abstract Cache<String, GridBucketState> getCache();

}
