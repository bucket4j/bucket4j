package io.github.bucket4j.grid.ignite.thin;

import java.nio.ByteBuffer;
import java.util.Objects;

import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientCompute;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.grid.ignite.thin.cas.IgniteThinClientCasBasedProxyManager;
import io.github.bucket4j.grid.ignite.thin.compute.IgniteThinClientProxyManager;

public class Bucket4jIgniteThin {

    public static final Bucket4jIgniteThin INSTANCE = new Bucket4jIgniteThin();

    /**
     * Returns the builder for {@link IgniteThinClientCasBasedProxyManager}
     *
     * @param cache
     *
     * @return new instance of {@link IgniteThinClientCasBasedProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public <K> IgniteThinClientCasBasedProxyManagerBuilder<K> casBasedBuilder(ClientCache<K, ByteBuffer> cache) {
        return new IgniteThinClientCasBasedProxyManagerBuilder<>(cache);
    }

    /**
     * Returns the builder for {@link IgniteThinClientProxyManager}
     *
     * @param cache
     * @param clientCompute
     *
     * @return new instance of {@link IgniteThinClientComputeProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public <K> IgniteThinClientComputeProxyManagerBuilder<K> clientComputeBasedBuilder(ClientCache<K, byte[]> cache, ClientCompute clientCompute) {
        return new IgniteThinClientComputeProxyManagerBuilder<>(cache, clientCompute);
    }

    public static class IgniteThinClientCasBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, IgniteThinClientCasBasedProxyManager<K>, IgniteThinClientCasBasedProxyManagerBuilder<K>> {

        private final ClientCache<K, ByteBuffer> cache;

        public IgniteThinClientCasBasedProxyManagerBuilder(ClientCache<K, ByteBuffer> cache) {
            this.cache = Objects.requireNonNull(cache);
        }

        @Override
        public IgniteThinClientCasBasedProxyManager<K> build() {
            return new IgniteThinClientCasBasedProxyManager<>(this);
        }

        public ClientCache<K, ByteBuffer> getCache() {
            return cache;
        }
    }

    public static class IgniteThinClientComputeProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, IgniteThinClientProxyManager<K>, IgniteThinClientComputeProxyManagerBuilder<K>> {

        private final ClientCache<K, byte[]> cache;
        private final ClientCompute clientCompute;

        public IgniteThinClientComputeProxyManagerBuilder(ClientCache<K, byte[]> cache, ClientCompute clientCompute) {
            this.cache = Objects.requireNonNull(cache);
            this.clientCompute = Objects.requireNonNull(clientCompute);
        }

        @Override
        public IgniteThinClientProxyManager<K> build() {
            return new IgniteThinClientProxyManager<>(this);
        }

        public ClientCache<K, byte[]> getCache() {
            return cache;
        }

        public ClientCompute getClientCompute() {
            return clientCompute;
        }
    }

}
