package io.github.bucket4j.caffeine;

import java.util.Objects;

import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

/**
 * Entry point for Caffeine integration
 */
public class Bucket4jCaffeine {

    /**
     * Returns the builder for {@link CaffeineProxyManager}
     *
     * @param cacheBuilder
     *
     * @return new instance of {@link CaffeineProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public static <K> CaffeineProxyManagerBuilder<K> builderFor(Caffeine<?, ?> cacheBuilder) {
        return new CaffeineProxyManagerBuilder<>((Caffeine) cacheBuilder);
    }

    public static class CaffeineProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, CaffeineProxyManager<K>, CaffeineProxyManagerBuilder<K>> {

        final Caffeine<K, RemoteBucketState> cacheBuilder;

        public CaffeineProxyManagerBuilder(Caffeine<K, RemoteBucketState> cacheBuilder) {
            this.cacheBuilder = Objects.requireNonNull(cacheBuilder);
        }

        @Override
        public CaffeineProxyManager<K> build() {
            return new CaffeineProxyManager<>(this);
        }

    }

}
