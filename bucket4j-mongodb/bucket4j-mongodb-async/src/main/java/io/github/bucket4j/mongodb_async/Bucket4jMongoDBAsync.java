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
package io.github.bucket4j.mongodb_async;

import com.mongodb.reactivestreams.client.MongoCollection;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.mongodb.AbstractMongoDBProxyManagerBuilder;
import org.bson.Document;

import static io.github.bucket4j.distributed.serialization.Mapper.STRING;

/**
 * Entry point for MongoDB integration that uses Reactive Streams-based MongoDB Java Driver from org.mongodb.mongodb-driver-reactivestreams.
 */
public class Bucket4jMongoDBAsync {
    /**
     * Return the builder for {@link MongoDBAsyncCompareAndSwapBasedProxyManager} that uses {@link String} values as keys by default.
     *
     * @param collection MongoDB collection that holds buckets.
     * @return New instance of {@link MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder}
     */
    public static MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder<String> compareAndSwapBasedBuilder(MongoCollection<Document> collection) {
        return new MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder<>(collection, STRING);
    }

    /**
     * Return the builder for {@link MongoDBAsyncCompareAndSwapBasedProxyManager} that uses values of type {@link K} as keys.
     *
     * @param collection MongoDB collection that holds buckets.
     * @param keyMapper  An implementation of interface {@link Mapper} that will be used for mapping keys from {@link K} to byte[].
     * @param <K>        The type that will be used for key mapping.
     * @return New instance of {@link MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder}
     */
    public static <K> MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder<K> compareAndSwapBasedBuilder(MongoCollection<Document> collection, Mapper<K> keyMapper) {
        return new MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder<>(collection, keyMapper);
    }

    public static class MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder<K> extends AbstractMongoDBProxyManagerBuilder<K, MongoDBAsyncCompareAndSwapBasedProxyManager<K>, MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder<K>> {
        private final MongoCollection<Document> collection;

        public MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder(MongoCollection<Document> collection, Mapper<K> keyMapper) {
            super(keyMapper);
            this.collection = collection;
        }

        public MongoCollection<Document> getCollection() {
            return collection;
        }

        @Override
        public MongoDBAsyncCompareAndSwapBasedProxyManager<K> build() {
            return new MongoDBAsyncCompareAndSwapBasedProxyManager<>(this);
        }
    }
}
