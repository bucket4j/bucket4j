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
import io.github.bucket4j.distributed.BucketProxy;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents an extension point of Bucket4j library.
 * {@link ProxyManager} provides API for building and managing the collection of {@link BucketProxy} in backing storage.
 * Typically, an instance of {@link ProxyManager} is organized around RDBMS table, GRID cache, or some similarly isolated part of external storage.
 * Primary keys are used to distinguish persisted state of different buckets.
 *
 * @param <K> type of primary key
 *
 * @see BucketProxy
 * @see AsyncProxyManager
 */
public interface ProxyManager<K> {

    /**
     * Creates bucket-proxy that configured by default parameters that set on proxy-manager level.
     * In case if you need to configure something special for bucket like implicit config replacement you should to use {@link #builder()} instead of this method.
     *
     * @param key the key that used in external storage to distinguish one bucket from another.
     * @param configurationSupplier provider for bucket configuration
     *
     * @return new instance of {@link BucketProxy}
     */
    default BucketProxy getProxy(K key, Supplier<BucketConfiguration> configurationSupplier) {
        return builder().build(key, configurationSupplier);
    }

    /**
     * Creates new instance of {@link RemoteBucketBuilder}.
     *
     * <p>
     * Use this method only if you need to create bucket with parameters that can not be specified via {@link AbstractProxyManagerBuilder},
     * otherwise prefer {@link #getProxy(Object, Supplier)}
     *
     * @return new instance of {@link RemoteBucketBuilder}
     */
    RemoteBucketBuilder<K> builder();

    /**
     * Locates configuration of bucket which actually stored in the underlying storage.
     *
     * @param key the unique identifier used to point to the bucket in external storage.
     *
     * @return Optional surround the configuration or empty optional if bucket with specified key is not stored.
     */
    Optional<BucketConfiguration> getProxyConfiguration(K key);

    /**
     * Removes persisted state of bucket from underlying storage.
     *
     * @param key the primary key of bucket which state need to be removed from underlying storage.
     */
    void removeProxy(K key);

    /**
     * Describes whether this manager supports asynchronous API.
     * If this method returns <code>false</code> then any invocation of {@link #asAsync()} will throw {@link UnsupportedOperationException}.
     *
     * @return <code>true</code> if this manager supports asynchronous API
     */
    boolean isAsyncModeSupported();

    /**
     * Describes whether this manager supports expire-after-write feature.
     *
     * @return <code>true</code> if this manager supports expire-after-write feature.
     */
    default boolean isExpireAfterWriteSupported() {
        return false;
    }

    /**
     * Returns asynchronous API for this proxy manager.
     *
     * @return asynchronous API for this proxy manager.
     *
     * @throws UnsupportedOperationException in case of this proxy manager does not support Async API.
     */
    AsyncProxyManager<K> asAsync() throws UnsupportedOperationException;

    /**
     * Returns a proxy object that wraps this ProxyManager such that keys are first mapped using the specified mapping function
     * before being sent to the remote store. The returned ProxyManager shares the same underlying store as the original,
     * and keys that map to the same value will share the same remote state.
     *
     * @param mapper the mapper function to apply to keys
     * @return a proxy object that wraps this ProxyManager
     * @param <K1> the type of key accepted by returned ProxyManager
     */
    default <K1> ProxyManager<K1> withMapper(Function<? super K1, ? extends K> mapper) {
        return new ProxyManagerView(this, mapper);
    }

}
