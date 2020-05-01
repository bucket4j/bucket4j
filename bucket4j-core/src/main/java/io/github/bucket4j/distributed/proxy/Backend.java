/*
 *
 * Copyright 2015-2019 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.distributed.proxy;
import io.github.bucket4j.BucketConfiguration;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * TODO javadocs
 * Represents an extension point of bucket4j library.
 *
 * Provides an light-weight proxy to bucket which state actually stored in any external storage outside current JVM,
 * like in-memory jvm or relational database.
 *
 * @param <K> type of key
 */
public interface Backend<K> {

    /**
     * TODO fix javadocs
     *
     * Constructs an instance of {@link BucketProxy} which state actually stored inside in-memory data-jvm,
     * the bucket stored in the jvm immediately, so one network request will be issued to jvm.
     * Due to this method performs network IO, returned result must not be treated as light-weight entity,
     * it will be a performance anti-pattern to use this method multiple times for same key,
     * you need to cache result somewhere and reuse between invocations,
     * else performance of all operation with bucket will be 2-x times slower.
     *
     * @return new distributed bucket
     */
    RemoteBucketBuilder<K> builder();

    /**
     * TODO
     *
     * Describes whether or not this backend supports asynchronous mode.
     *
     * @return true if this extension supports asynchronous mode.
     */
    boolean isAsyncModeSupported();

    /**
     * Locates configuration of bucket which actually stored outside current JVM.
     *
     * @param key the unique identifier used to point to the bucket in external storage.
     *
     * @return Optional surround the configuration or empty optional if bucket with specified key are not stored.
     */
    Optional<BucketConfiguration> getProxyConfiguration(K key);

    /**
     * TODO
     *
     * Locates configuration of bucket which actually stored outside current JVM.
     *
     * @param key the unique identifier used to point to the bucket in external storage.
     *
     * @return Optional surround the configuration or empty optional if bucket with specified key are not stored.
     */
    CompletableFuture<Optional<BucketConfiguration>> getProxyConfigurationAsync(K key);

}
