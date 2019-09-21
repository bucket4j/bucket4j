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

import io.github.bucket4j.AsyncBucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.commands.GetConfigurationCommand;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Represents an extension point of bucket4j library.
 *
 * TODO javadocs
 *
 * @param <K>
 */
/**
 * TODO javadocs
 *
 * Provides an light-weight proxy to bucket which state actually stored in any external storage outside current JVM,
 * like in-memory jvm or relational database.
 *
 * The proxies instantiated by ProxyManager is very cheap, you are free to instantiate as many proxies as you wish,
 * there are no any hard work performed inside {@link #getProxy(K, Supplier) getProxy} method,
 * so it is not necessary to cache results of its invocation.
 *
 * @param <K> type of key
 */
public interface Backend<K extends Serializable> {

    /**
     * TODO fix javadocs
     *
     * Provides light-weight proxy to bucket which actually stored outside current JVM.
     * This method do not perform any hard work or network calls, it is not necessary to cache results of its invocation.
     *
     * @param key the unique identifier used to point to the bucket in external storage.
     * @param configurationSupplier supplier for configuration which can be called to build bucket configuration,
     *                                  if and only if first invocation of any method on proxy detects that bucket absents in remote storage,
     *                                  in this case provide configuration will be used to instantiate and persist the missed bucket.
     *
     * @return proxy to bucket that can be actually stored outside current JVM.
     */
    BucketProxy getProxy(K key, Supplier<BucketConfiguration> configurationSupplier);

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
     * <p>
     * Use this method if and only if you need to full control over bucket lifecycle(especially specify {@link RecoveryStrategy}),
     * and you have clean caching strategy which suitable for storing buckets,
     * else it would be better to work through {@link JCache#proxyManagerForCache(Cache) ProxyManager},
     * which does not require any caching, because ProxyManager operates with light-weight versions of buckets.
     *
     * @param cache distributed cache which will hold bucket inside cluster.
     *             Feel free to store inside single {@code cache} as mush buckets as you need.
     * @param key  for storing bucket inside {@code cache}.
     *             If you plan to store multiple buckets inside single {@code cache}, then each bucket should has own unique {@code key}.
     * @param recoveryStrategy specifies the reaction which should be applied in case of previously saved state of bucket has been lost.
     *
     * @return new distributed bucket
     */
    BucketProxy<K> getDurableProxy(K key, Supplier<BucketConfiguration> configurationSupplier, RecoveryStrategy recoveryStrategy);

    /**
     * Locates configuration of bucket which actually stored outside current JVM.
     *
     * @param key the unique identifier used to point to the bucket in external storage.
     *
     * @return Optional surround the configuration or empty optional if bucket with specified key are not stored.
     */
    Optional<BucketConfiguration> getProxyConfiguration(K key);

}
