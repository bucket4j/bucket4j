package io.github.bucket4j.grid.infinispan;

import java.util.Objects;

import org.infinispan.functional.FunctionalMap;


import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;

/**
 * Entry point for Infinispan integration
 */
public class Bucket4jInfinispan {

    /**
     * Returns the builder for {@link InfinispanProxyManager}
     *
     * @param readWriteMap
     *
     * @return new instance of {@link InfinispanProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public static <K> InfinispanProxyManagerBuilder<K> builderFor(FunctionalMap.ReadWriteMap<K, byte[]> readWriteMap) {
        return new InfinispanProxyManagerBuilder<>(readWriteMap);
    }

    public static class InfinispanProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, InfinispanProxyManager<K>, InfinispanProxyManagerBuilder<K>> {

        final FunctionalMap.ReadWriteMap<K, byte[]> readWriteMap;

        public InfinispanProxyManagerBuilder(FunctionalMap.ReadWriteMap<K, byte[]> readWriteMap) {
            this.readWriteMap = Objects.requireNonNull(readWriteMap);
        }

        @Override
        public InfinispanProxyManager<K> build() {
            return new InfinispanProxyManager<>(this);
        }

    }

}
