/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
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

package io.github.bucket4j.remote;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Provides an light-weight proxy to bucket which state actually stored in any external storage outside current JVM,
 * like in-memory jvm or relational database.
 *
 * The proxies instantiated by ProxyManager is very cheap, you are free to instantiate as many proxies as you wish,
 * there are no any hard work performed inside {@link #getProxy(K, Supplier) getProxy} method,
 * so it is not necessary to cache results of its invocation.
 * 
 * @param <K> type of key
 */
public class ProxyManager<K extends Serializable> {

    private final Backend<K> backend;

    // TODO javadocs
    public ProxyManager(Backend<K> backend) {
        this.backend = Objects.requireNonNull(backend);
    }

    /**
     * Provides light-weight proxy to bucket which actually stored outside current JVM.
     * This method do not perform any hard work or network calls, it is not necessary to cache results of its invocation.
     *
     * <p>Use this method if you use same configuration for all buckets(it can be easy stored as constant),
     * or configuration is very cheap to create.
     * </p>
     *
     * @param key the unique identifier used to point to the bucket in external storage.
     * @param configuration this configuration will be used to initialize bucket if it is not initialized before
     *
     * @return proxy to bucket that can be actually stored outside current JVM.
     */
    public Bucket getProxy(K key, BucketConfiguration configuration) {
        return getProxy(key, () -> configuration);
    }

    /**
     * Provides light-weight proxy to bucket which actually stored outside current JVM.
     * This method do not perform any hard work or network calls, it is not necessary to cache results of its invocation.
     *
     * <p>Use this method in case of different configurations for each bucket and configuration is very expensive to create.
     * </p>
     *
     * @param key the unique identifier used to point to the bucket in external storage.
     * @param configurationLazySupplier supplier for configuration which can be called to build bucket configuration,
     *                                  if and only if first invocation of any method on proxy detects that bucket absents in remote storage,
     *                                  in this case provide configuration will be used to instantiate and persist the missed bucket.
     *
     * @return proxy to bucket that can be actually stored outside current JVM.
     */
    public Bucket getProxy(K key, Supplier<BucketConfiguration> configurationLazySupplier) {
        return BucketProxy.createLazyBucket(key, configurationLazySupplier, backend);
    }

    /**
     * Locates proxy to bucket which actually stored outside current JVM.
     *
     * @param key the unique identifier used to point to the bucket in external storage.
     *
     * @return Optional surround the proxy to bucket or empty optional if bucket with specified key are not stored.
     */
    public Optional<Bucket> getProxy(K key) {
        return getProxyConfiguration(key)
                .map(configuration -> BucketProxy.createLazyBucket(key, () -> configuration, backend));
    }

    /**
     * Locates configuration of bucket which actually stored outside current JVM.
     *
     * @param key the unique identifier used to point to the bucket in external storage.
     *
     * @return Optional surround the configuration or empty optional if bucket with specified key are not stored.
     */
    public Optional<BucketConfiguration> getProxyConfiguration(K key) {
        return backend.getConfiguration(key);
    }

}
