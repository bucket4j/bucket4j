package io.github.bucket4j.redis.glide;

import glide.api.BaseClient;
import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.glide.cas.GlideBasedProxyManager;

import java.util.Objects;

/**
 * Entry point for Glide integration
 */
public class Bucket4jGlide {

    /**
     * Returns the builder for {@link GlideBasedProxyManager}
     *
     * @param client
     * @return new instance of {@link GlideBasedProxyManagerBuilder}
     */
    public static GlideBasedProxyManagerBuilder<byte[]> casBasedBuilder(BaseClient client) {
        return new GlideBasedProxyManagerBuilder<>(Mapper.BYTES, client);
    }

    public static class GlideBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, GlideBasedProxyManager<K>, GlideBasedProxyManagerBuilder<K>> {

        private final BaseClient client;
        private Mapper<K> keyMapper;

        public GlideBasedProxyManagerBuilder(Mapper<K> keyMapper, BaseClient client) {
            this.client = Objects.requireNonNull(client);
            this.keyMapper = Objects.requireNonNull(keyMapper);
        }

        public Mapper<K> getKeyMapper() {
            return keyMapper;
        }

        public BaseClient getClient() {
            return client;
        }

        public <K2> GlideBasedProxyManagerBuilder<K2> keyMapper(Mapper<K2> keyMapper) {
            this.keyMapper = (Mapper) Objects.requireNonNull(keyMapper);
            return (GlideBasedProxyManagerBuilder<K2>) this;
        }

        @Override
        public GlideBasedProxyManager<K> build() {
            return new GlideBasedProxyManager<>(this);
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }

    }
}
