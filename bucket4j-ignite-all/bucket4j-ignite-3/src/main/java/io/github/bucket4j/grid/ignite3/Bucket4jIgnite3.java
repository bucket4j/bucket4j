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

import java.util.Objects;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.KeyValueView;

import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.grid.ignite3.cas.IgniteKeyValueCasBasedProxyManager;
import io.github.bucket4j.grid.ignite3.compute.IgniteComputeProxyManager;

/**
 * Entry point for Apache Ignite 3.x integration.
 */
public class Bucket4jIgnite3 {

    public static final Bucket4jIgnite3 INSTANCE = new Bucket4jIgnite3();

    /**
     * Returns the builder for {@link IgniteKeyValueCasBasedProxyManager}.
     *
     * <p>
     * This proxy manager performs a plain get/compare-and-swap dialog with the supplied {@link KeyValueView},
     * without executing any code on the Ignite server side. Use this option when you already own a table
     * and want the simplest possible integration.
     *
     * @param keyValueView key-value view of a table where the bucket state is stored as {@code byte[]}
     * @param <K> type of key
     *
     * @return new instance of {@link IgniteKeyValueCasBasedProxyManagerBuilder}
     */
    public <K> IgniteKeyValueCasBasedProxyManagerBuilder<K> casBasedBuilder(KeyValueView<K, byte[]> keyValueView) {
        return new IgniteKeyValueCasBasedProxyManagerBuilder<>(keyValueView);
    }

    /**
     * Returns the builder for {@link IgniteComputeProxyManager}.
     *
     * <p>
     * This proxy manager executes a colocated {@link org.apache.ignite.compute.ComputeJob} on the cluster node
     * that owns the bucket's partition, so the read-modify-write cycle happens without extra network hops.
     * The job class(and bucket4j-core) must be reachable from the server node's classloader,
     * either by placing the corresponding jars on the node's classpath, or via Ignite's Deployment Units.
     *
     * @param client thin client connected to the cluster
     * @param tableSettings names of the table and columns to use as a bucket store
     *
     * @return new instance of {@link IgniteComputeProxyManagerBuilder}
     */
    public IgniteComputeProxyManagerBuilder computeBasedBuilder(IgniteClient client, BucketTableSettings tableSettings) {
        return new IgniteComputeProxyManagerBuilder(client, tableSettings);
    }

    public static class IgniteKeyValueCasBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, IgniteKeyValueCasBasedProxyManager<K>, IgniteKeyValueCasBasedProxyManagerBuilder<K>> {

        private final KeyValueView<K, byte[]> keyValueView;

        public IgniteKeyValueCasBasedProxyManagerBuilder(KeyValueView<K, byte[]> keyValueView) {
            this.keyValueView = Objects.requireNonNull(keyValueView);
        }

        @Override
        public IgniteKeyValueCasBasedProxyManager<K> build() {
            return new IgniteKeyValueCasBasedProxyManager<>(this);
        }

        public KeyValueView<K, byte[]> getKeyValueView() {
            return keyValueView;
        }
    }

    public static class IgniteComputeProxyManagerBuilder extends AbstractProxyManagerBuilder<String, IgniteComputeProxyManager, IgniteComputeProxyManagerBuilder> {

        private final IgniteClient client;
        private final BucketTableSettings tableSettings;

        public IgniteComputeProxyManagerBuilder(IgniteClient client, BucketTableSettings tableSettings) {
            this.client = Objects.requireNonNull(client);
            this.tableSettings = Objects.requireNonNull(tableSettings);
        }

        @Override
        public IgniteComputeProxyManager build() {
            return new IgniteComputeProxyManager(this);
        }

        public IgniteClient getClient() {
            return client;
        }

        public BucketTableSettings getTableSettings() {
            return tableSettings;
        }
    }

}
