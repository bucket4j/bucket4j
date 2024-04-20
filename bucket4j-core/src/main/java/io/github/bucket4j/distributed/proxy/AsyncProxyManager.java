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
import java.util.function.Function;
import java.util.function.Supplier;

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
     * Creates bucket-proxy that configured by default parameters that set on proxy-manager level.
     *
     * @param key the key that used in external storage to distinguish one bucket from another.
     * @param configurationSupplier provider for bucket configuration
     *
     * @return new instance of {@link AsyncBucketProxy}
     */
    default AsyncBucketProxy getProxy(K key, Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier) {
        return builder().build(key, configurationSupplier);
    }

    /**
     * Creates new instance of {@link RemoteAsyncBucketBuilder}
     *
     * <p>
     * Use this method only if you need to create bucket with parameters that can not be specified via {@link AbstractProxyManagerBuilder},
     * otherwise prefer {@link #getProxy(Object, Supplier)}
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

    /**
     * Returns a proxy object that wraps this AsyncProxyManager such that keys are first mapped using the specified mapping function
     * before being sent to the remote store. The returned AsyncProxyManager shares the same underlying store as the original,
     * and keys that map to the same value will share the same remote state.
     *
     * @param mapper the mapper function to apply to keys
     * @return a proxy object that wraps this AsyncProxyManager
     * @param <K1> the type of key accepted by returned AsyncProxyManager
     */
    default <K1> AsyncProxyManager<K1> withMapper(Function<? super K1, ? extends K> mapper) {
        return new AsyncProxyManagerView(this, mapper);
    }

}
