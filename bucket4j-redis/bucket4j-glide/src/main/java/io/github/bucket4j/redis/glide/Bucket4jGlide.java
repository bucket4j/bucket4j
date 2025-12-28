/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2025 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
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
