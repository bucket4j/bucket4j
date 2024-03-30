package io.github.bucket4j.grid.jcache;

import java.util.Objects;

import javax.cache.Cache;


import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;

/**
 * Entry point for JCache integration
 */
public class Bucket4jJCache {

    /**
     * Returns the builder for {@link JCacheProxyManager}
     *
     * @param cache
     *
     * @return new instance of {@link JCacheProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public static <K> JCacheProxyManagerBuilder<K> builderFor(Cache<K, byte[]> cache) {
        return new JCacheProxyManagerBuilder<>(cache);
    }

    public static class JCacheProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, JCacheProxyManager<K>, JCacheProxyManagerBuilder<K>> {

        final Cache<K, byte[]> cache;

        public JCacheProxyManagerBuilder(Cache<K, byte[]> cache) {
            this.cache = Objects.requireNonNull(cache);
        }

        @Override
        public JCacheProxyManager<K> build() {
            return new JCacheProxyManager<>(this);
        }

    }

}
