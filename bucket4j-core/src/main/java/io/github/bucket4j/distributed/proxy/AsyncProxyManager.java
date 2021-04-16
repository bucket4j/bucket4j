/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
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
package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.AsyncBucketProxy;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The asynchronous equivalent of {@link ProxyManager}.
 *
 * @param <K> type of primary key
 *
 * @see AsyncBucketProxy
 * @see ProxyManager
 */
public interface AsyncProxyManager<K> {

    /**
     * Creates new instance of {@link RemoteAsyncBucketBuilder}
     *
     * @return new instance of {@link RemoteAsyncBucketBuilder}
     */
    RemoteAsyncBucketBuilder<K> builder();

    /**
     * Asynchronously removes persisted state of bucket from underlying storage.
     *
     * @param key the primary key of bucket which state need to be removed from underlying storage.
     * @return the future that will be completed after deletion
     */
    CompletableFuture<Void> removeProxy(K key);

    /**
     * Asynchronously locates configuration of bucket which actually stored in the underlying storage.
     *
     * @param key the unique identifier used to point to the bucket in external storage.
     *
     * @return The future that completed by optional surround the configuration or empty optional if bucket with specified key is not stored.
     */
    CompletableFuture<Optional<BucketConfiguration>> getProxyConfiguration(K key);

}
