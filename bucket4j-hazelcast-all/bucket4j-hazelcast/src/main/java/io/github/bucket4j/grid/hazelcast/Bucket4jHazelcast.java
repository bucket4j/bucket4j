package io.github.bucket4j.grid.hazelcast;

import java.util.Objects;

import com.hazelcast.map.IMap;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;

/**
 * Entry point for Hazelcast integration
 */
public class Bucket4jHazelcast {

    /**
     * Returns the builder for {@link HazelcastProxyManager}
     *
     * @param map
     *
     * @return new instance of {@link HazelcastProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public static <K> HazelcastProxyManagerBuilder<K> entryProcessorBasedBuilder(IMap<K, byte[]> map) {
        return new HazelcastProxyManagerBuilder<>(map);
    }

    /**
     * Returns the builder for {@link HazelcastLockBasedProxyManager}
     *
     * @param map
     *
     * @return new instance of {@link HazelcastLockBasedProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public static <K> HazelcastLockBasedProxyManagerBuilder<K> lockBasedBuilder(IMap<K, byte[]> map) {
        return new HazelcastLockBasedProxyManagerBuilder<>(map);
    }

    /**
     * Returns the builder for {@link HazelcastCompareAndSwapBasedProxyManager}
     *
     * @param map
     *
     * @return new instance of {@link HazelcastCompareAndSwapBasedProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public static <K> HazelcastCompareAndSwapBasedProxyManagerBuilder<K> casBasedBuilder(IMap<K, byte[]> map) {
        return new HazelcastCompareAndSwapBasedProxyManagerBuilder<>(map);
    }

    public static class HazelcastProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, HazelcastProxyManager<K>, HazelcastProxyManagerBuilder<K>> {

        final IMap<K, byte[]> map;
        String offloadableExecutorName;

        public HazelcastProxyManagerBuilder(IMap<K, byte[]> map) {
            this.map = Objects.requireNonNull(map);
        }

        public HazelcastProxyManagerBuilder<K> offloadableExecutorName(String offloadableExecutorName) {
            this.offloadableExecutorName = Objects.requireNonNull(offloadableExecutorName);
            return this;
        }

        @Override
        public HazelcastProxyManager<K> build() {
            return new HazelcastProxyManager<>(this);
        }
    }

    public static class HazelcastCompareAndSwapBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, HazelcastCompareAndSwapBasedProxyManager<K>, HazelcastCompareAndSwapBasedProxyManagerBuilder<K>> {

        final IMap<K, byte[]> map;

        public HazelcastCompareAndSwapBasedProxyManagerBuilder(IMap<K, byte[]> map) {
            this.map = Objects.requireNonNull(map);
        }

        @Override
        public HazelcastCompareAndSwapBasedProxyManager<K> build() {
            return new HazelcastCompareAndSwapBasedProxyManager<>(this);
        }
    }

    public static class HazelcastLockBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, HazelcastLockBasedProxyManager<K>, HazelcastLockBasedProxyManagerBuilder<K>> {

        final IMap<K, byte[]> map;

        public HazelcastLockBasedProxyManagerBuilder(IMap<K, byte[]> map) {
            this.map = Objects.requireNonNull(map);
        }

        @Override
        public HazelcastLockBasedProxyManager<K> build() {
            return new HazelcastLockBasedProxyManager<>(this);
        }
    }

}
