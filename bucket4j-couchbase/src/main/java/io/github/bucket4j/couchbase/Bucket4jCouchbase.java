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
package io.github.bucket4j.couchbase;

import com.couchbase.client.java.AsyncCollection;
import com.couchbase.client.java.Collection;
import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.serialization.Mapper;

import java.util.Objects;

import static io.github.bucket4j.distributed.serialization.Mapper.STRING;

/**
 * Entry point for Couchbase integration that uses the Couchbase Java SDK.
 */
public class Bucket4jCouchbase {

    /**
     * Returns the builder for {@link CouchbaseCompareAndSwapBasedProxyManager}
     *
     * @param collection Couchbase collection that holds buckets.
     *
     * @return new instance of {@link CouchbaseCompareAndSwapBasedProxyManagerBuilder}
     */
    public static CouchbaseCompareAndSwapBasedProxyManagerBuilder<String> compareAndSwapBasedBuilder(Collection collection) {
        return new CouchbaseCompareAndSwapBasedProxyManagerBuilder<>(collection, STRING);
    }

    /**
     * Returns the builder for {@link CouchbaseCompareAndSwapBasedProxyManager}
     *
     * <p>
     * Use this overload when the application already works with the asynchronous Couchbase API and
     * does not want to keep a reference to the blocking {@link Collection} facade.
     *
     * @param collection Couchbase async collection that holds buckets.
     *
     * @return new instance of {@link CouchbaseCompareAndSwapBasedProxyManagerBuilder}
     */
    public static CouchbaseCompareAndSwapBasedProxyManagerBuilder<String> compareAndSwapBasedBuilder(AsyncCollection collection) {
        return new CouchbaseCompareAndSwapBasedProxyManagerBuilder<>(collection, STRING);
    }

    /**
     * Returns the builder for {@link CouchbaseCompareAndSwapBasedProxyManager}
     *
     * @param collection Couchbase collection that holds buckets.
     * @param keyMapper object responsible for mapping keys from {@link K} to document ids.
     * @param <K> type of primary key
     *
     * @return new instance of {@link CouchbaseCompareAndSwapBasedProxyManagerBuilder}
     */
    public static <K> CouchbaseCompareAndSwapBasedProxyManagerBuilder<K> compareAndSwapBasedBuilder(Collection collection, Mapper<K> keyMapper) {
        return new CouchbaseCompareAndSwapBasedProxyManagerBuilder<>(collection, keyMapper);
    }

    /**
     * Returns the builder for {@link CouchbaseCompareAndSwapBasedProxyManager}
     *
     * <p>
     * Use this overload when the application already works with the asynchronous Couchbase API and
     * does not want to keep a reference to the blocking {@link Collection} facade.
     *
     * @param collection Couchbase async collection that holds buckets.
     * @param keyMapper object responsible for mapping keys from {@link K} to document ids.
     * @param <K> type of primary key
     *
     * @return new instance of {@link CouchbaseCompareAndSwapBasedProxyManagerBuilder}
     */
    public static <K> CouchbaseCompareAndSwapBasedProxyManagerBuilder<K> compareAndSwapBasedBuilder(AsyncCollection collection, Mapper<K> keyMapper) {
        return new CouchbaseCompareAndSwapBasedProxyManagerBuilder<>(collection, keyMapper);
    }

    public static class CouchbaseCompareAndSwapBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, CouchbaseCompareAndSwapBasedProxyManager<K>, CouchbaseCompareAndSwapBasedProxyManagerBuilder<K>> {

        private final Collection collection;
        private final AsyncCollection asyncCollection;
        private Mapper<K> keyMapper;

        public CouchbaseCompareAndSwapBasedProxyManagerBuilder(Collection collection, Mapper<K> keyMapper) {
            this.collection = Objects.requireNonNull(collection);
            this.asyncCollection = collection.async();
            this.keyMapper = Objects.requireNonNull(keyMapper);
        }

        public CouchbaseCompareAndSwapBasedProxyManagerBuilder(AsyncCollection collection, Mapper<K> keyMapper) {
            this.collection = null;
            this.asyncCollection = Objects.requireNonNull(collection);
            this.keyMapper = Objects.requireNonNull(keyMapper);
        }

        public Collection getCollection() {
            return collection;
        }

        public AsyncCollection getAsyncCollection() {
            return asyncCollection;
        }

        /**
         * Specifies the type of key.
         *
         * @param keyMapper object responsible for converting primary keys to Couchbase document ids.
         *
         * @return this builder instance
         */
        public <K2> CouchbaseCompareAndSwapBasedProxyManagerBuilder<K2> keyMapper(Mapper<K2> keyMapper) {
            this.keyMapper = (Mapper) Objects.requireNonNull(keyMapper);
            return (CouchbaseCompareAndSwapBasedProxyManagerBuilder<K2>) this;
        }

        public Mapper<K> getKeyMapper() {
            return keyMapper;
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }

        @Override
        public CouchbaseCompareAndSwapBasedProxyManager<K> build() {
            return new CouchbaseCompareAndSwapBasedProxyManager<>(this);
        }
    }
}
