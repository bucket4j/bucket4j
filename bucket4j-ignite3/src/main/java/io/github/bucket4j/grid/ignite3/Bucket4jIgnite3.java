/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2026 Vladimir Bukhtoyarov
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
package io.github.bucket4j.grid.ignite3;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.grid.ignite3.internal.KeyCodec;
import org.apache.ignite.Ignite;

import java.util.Objects;

/**
 * Entry point for building {@link Ignite3ProxyManager} instances backed by an Apache Ignite 3.x table.
 *
 * <p>The target table must already exist and have a column named {@code "value"} of type {@code VARBINARY}
 * (or {@code BLOB}) to hold the serialized bucket state, in addition to its primary key column(s).
 */
public final class Bucket4jIgnite3 {

    private Bucket4jIgnite3() {
    }

    public static <K> Ignite3ProxyManagerBuilder<K> builder() {
        return new Ignite3ProxyManagerBuilder<>();
    }

    public static final class Ignite3ProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, Ignite3ProxyManager<K>, Ignite3ProxyManagerBuilder<K>> {

        private Ignite ignite;
        private String tableName;
        private Class<K> keyType;

        /**
         * Configures the {@link Ignite} instance (embedded node or thin client) used to reach the target table.
         */
        public Ignite3ProxyManagerBuilder<K> ignite(Ignite ignite) {
            this.ignite = Objects.requireNonNull(ignite);
            return this;
        }

        /**
         * Configures the name of the table that holds bucket state.
         */
        public Ignite3ProxyManagerBuilder<K> table(String tableName) {
            this.tableName = Objects.requireNonNull(tableName);
            return this;
        }

        /**
         * Configures the type of the table's primary key column.
         *
         * <p>Only {@code String}, {@code Long}, {@code Integer}, {@code Short}, {@code Byte} and {@code UUID} are
         * supported, because the key is both routed to Ignite's {@code Mapper.of(Class)} for compute-job colocation
         * and packed into the job's binary argument by bucket4j-ignite3 itself.
         */
        public Ignite3ProxyManagerBuilder<K> keyType(Class<K> keyType) {
            KeyCodec.requireSupported(Objects.requireNonNull(keyType));
            this.keyType = keyType;
            return this;
        }

        Ignite getIgnite() {
            return ignite;
        }

        String getTableName() {
            return tableName;
        }

        Class<K> getKeyType() {
            return keyType;
        }

        @Override
        public Ignite3ProxyManager<K> build() {
            Objects.requireNonNull(ignite, "ignite must be specified");
            Objects.requireNonNull(tableName, "table must be specified");
            Objects.requireNonNull(keyType, "keyType must be specified");
            return new Ignite3ProxyManager<>(this);
        }

    }

}
