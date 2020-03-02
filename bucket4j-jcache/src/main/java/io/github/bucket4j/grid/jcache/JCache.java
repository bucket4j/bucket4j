
package io.github.bucket4j.grid.jcache;

import io.github.bucket4j.Extension;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.serialization.SerializationHandle;

import javax.cache.Cache;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

/**
 * The extension of Bucket4j library addressed to support <a href="https://www.jcp.org/en/jsr/detail?id=107">JCache API (JSR 107)</a> specification.
 */
public class JCache implements Extension<JCacheBucketBuilder> {

    /**
     * {@inheritDoc}
     *
     * @return new instance of {@link JCacheBucketBuilder}
     */
    @Override
    public JCacheBucketBuilder builder() {
        return new JCacheBucketBuilder();
    }

    /**
     * Creates {@link JCacheProxyManager} for specified cache.
     *
     * @param cache cache for storing state of buckets
     * @param <T> type of keys in the cache
     * @return {@link ProxyManager} for specified cache.
     */
    public <T extends Serializable> ProxyManager<T> proxyManagerForCache(Cache<T, GridBucketState> cache) {
        return new JCacheProxyManager<>(cache);
    }

    @Override
    public Collection<SerializationHandle<?>> getSerializers() {
        return Arrays.asList(ExecuteProcessor.SERIALIZATION_HANDLE, InitStateProcessor.SERIALIZATION_HANDLE, InitStateAndExecuteProcessor.SERIALIZATION_HANDLE);
    }

}
