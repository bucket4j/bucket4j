package io.github.bucket4j.grid.coherence;

import java.util.Objects;

import com.tangosol.net.NamedCache;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;

/**
 * Entry point for Coherence integration
 */
public class Bucket4jCoherence {

    /**
     * Returns the builder for {@link CoherenceProxyManager}
     *
     * @param cache
     *
     * @return new instance of {@link CoherenceProxyManagerBuilder}
     * @param <K> type ok key
     */
    public static <K> CoherenceProxyManagerBuilder<K> entryProcessorBasedBuilder(NamedCache<K, byte[]> cache) {
        return new CoherenceProxyManagerBuilder<>(cache);
    }

    public static class CoherenceProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, CoherenceProxyManager<K>, CoherenceProxyManagerBuilder<K>> {

        final NamedCache<K, byte[]> cache;

        public CoherenceProxyManagerBuilder(NamedCache<K, byte[]> cache) {
            this.cache = Objects.requireNonNull(cache);
        }

        @Override
        public CoherenceProxyManager<K> build() {
            return new CoherenceProxyManager<>(this);
        }

    }

}
