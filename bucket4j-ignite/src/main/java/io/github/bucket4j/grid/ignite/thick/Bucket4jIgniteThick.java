package io.github.bucket4j.grid.ignite.thick;

import java.util.Objects;

import org.apache.ignite.IgniteCache;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;

public class Bucket4jIgniteThick {

    public static final Bucket4jIgniteThick INSTANCE = new Bucket4jIgniteThick();

    /**
     * Returns the builder for {@link IgniteProxyManager}
     *
     * @param cache
     *
     * @return new instance of {@link IgniteProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public static <K> IgniteProxyManagerBuilder<K> builderFor(IgniteCache<K, byte[]> cache) {
        return new IgniteProxyManagerBuilder<>(cache
        );
    }

    public static class IgniteProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, IgniteProxyManager<K>, IgniteProxyManagerBuilder<K>> {

        final IgniteCache<K, byte[]> cache;

        public IgniteProxyManagerBuilder(IgniteCache<K, byte[]> cache) {
            this.cache = Objects.requireNonNull(cache);
        }

        @Override
        public IgniteProxyManager<K> build() {
            return new IgniteProxyManager<>(this);
        }

    }



}
