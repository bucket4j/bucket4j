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

/**
 * Represents an extension point of bucket4j library.
 * {@link ProxyManager} provides API for building and managing the collection of {@link BucketProxy} in backing storage.
 * Typically an instance of {@link ProxyManager} is organized around RDBMS table, GRID cache, or some similarly isolated part of external storage.
 * Primary keys are used to distinguish persisted state of different buckets.
 *
 * @param <K> type of primary key
 *
 * @see BucketProxy
 * @see AsyncProxyManager
 */
public interface ProxyManager<K> {

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
     * Describes whether or not this manager supports asynchronous API.
     * If this method returns <code>false</code> then any invocation of {@link #asAsync()} will throw {@link UnsupportedOperationException}.
     *
     * @return <code>true</code> if this extension supports asynchronous API
     */
    boolean isAsyncModeSupported();

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
        return new ProxyManager<>() {
            @Override
            public RemoteBucketBuilder<K1> builder() {
                return ProxyManager.this.builder().withMapper(mapper);
            }

            @Override
            public Optional<BucketConfiguration> getProxyConfiguration(K1 key) {
                return ProxyManager.this.getProxyConfiguration(mapper.apply(key));
            }

            @Override
            public void removeProxy(K1 key) {
                ProxyManager.this.removeProxy(mapper.apply(key));
            }

            @Override
            public boolean isAsyncModeSupported() {
                return ProxyManager.this.isAsyncModeSupported();
            }

            @Override
            public AsyncProxyManager<K1> asAsync() throws UnsupportedOperationException {
                return ProxyManager.this.asAsync().withMapper(mapper);
            }

            // To prevent nesting of anonymous class instances, directly map the original instance.
            @Override
            public <K2> ProxyManager<K2> withMapper(Function<? super K2, ? extends K1> innerMapper) {
                return ProxyManager.this.withMapper(mapper.compose(innerMapper));
            }
        };
    }
}
