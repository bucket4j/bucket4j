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
 * <p>The target table must already exist, bucket4j-ignite3 never creates or alters it. It must contain:
 * <ul>
 *   <li>the primary-key column {@value Ignite3ProxyManager#KEY_COLUMN_NAME}, whose type matches the key type
 *       configured via {@link Ignite3ProxyManagerBuilder#keyType(Class)};</li>
 *   <li>the column {@value Ignite3ProxyManager#STATE_COLUMN_NAME} of type {@code VARBINARY}, which holds the
 *       serialized bucket state.</li>
 * </ul>
 *
 * <p>Bucket operations are executed by a compute job colocated with the bucket's key, so the bucket4j jars must
 * be present on the classpath of every server node of the cluster, not only on the client.
 *
 * <p>Per-entry expiration is not supported yet: configuring {@code expirationAfterWrite(...)} makes
 * {@link Ignite3ProxyManagerBuilder#build()} fail. Stale rows can be removed with
 * {@link Ignite3ProxyManager#removeProxy(Object)}.
 *
 * <pre>{@code
 * Ignite3ProxyManager<String> proxyManager = Bucket4jIgnite3.<String>builder()
 *         .ignite(ignite)
 *         .table("bucket4j_buckets")
 *         .keyType(String.class)
 *         .build();
 * }</pre>
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
         * Configures the {@link Ignite} instance (embedded node or thin client) used to reach the target table
         * and to submit compute jobs.
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

        /**
         * Builds the proxy manager.
         *
         * @throws NullPointerException if {@link #ignite(Ignite)}, {@link #table(String)} or {@link #keyType(Class)} was not called
         * @throws IllegalArgumentException if the configured table does not exist
         * @throws UnsupportedOperationException if {@code expirationAfterWrite(...)} was configured, because it is not supported
         */
        @Override
        public Ignite3ProxyManager<K> build() {
            Objects.requireNonNull(ignite, "ignite must be specified");
            Objects.requireNonNull(tableName, "table must be specified");
            Objects.requireNonNull(keyType, "keyType must be specified");
            return new Ignite3ProxyManager<>(this);
        }

    }

}
