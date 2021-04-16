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
import io.github.bucket4j.distributed.BucketProxy;

import java.util.Optional;

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

}
