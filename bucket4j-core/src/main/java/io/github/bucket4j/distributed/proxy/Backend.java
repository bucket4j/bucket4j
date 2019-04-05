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

import io.github.bucket4j.BackendOptions;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConfigurationBuilder;
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
     * Creates new instance of {@link ConfigurationBuilder}
     *
     * @return instance of {@link ConfigurationBuilder}
     */
    default ConfigurationBuilder configurationBuilder() {
        return new ConfigurationBuilder(getOptions());
    }

    /**
     * Creates new instance of builder specific for this back-end.
     *
     * @return new builder instance
     */
    default RemoteBucketBuilder<K> builder() {
        return new RemoteBucketBuilder<>(this);
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
    default Bucket getProxy(K key, BucketConfiguration configuration) {
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
    default Bucket getProxy(K key, Supplier<BucketConfiguration> configurationLazySupplier) {
        return BucketProxy.createLazyBucket(key, configurationLazySupplier, this);
    }

    /**
     * Locates proxy to bucket which actually stored outside current JVM.
     *
     * @param key the unique identifier used to point to the bucket in external storage.
     *
     * @return Optional surround the proxy to bucket or empty optional if bucket with specified key are not stored.
     */
    default Optional<Bucket> getProxy(K key) {
        return getProxyConfiguration(key)
                .map(configuration -> BucketProxy.createLazyBucket(key, () -> configuration, this));
    }

    /**
     * Locates configuration of bucket which actually stored outside current JVM.
     *
     * @param key the unique identifier used to point to the bucket in external storage.
     *
     * @return Optional surround the configuration or empty optional if bucket with specified key are not stored.
     */
    default Optional<BucketConfiguration> getProxyConfiguration(K key) {
        GetConfigurationCommand cmd = new GetConfigurationCommand();
        CommandResult<BucketConfiguration> result = this.execute(key, cmd);
        if (result.isBucketNotFound()) {
            return Optional.empty();
        }
        return Optional.of(result.getData());
    }

    // TODO javadocs
    <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command);

    // TODO javadocs
    <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command);

    // TODO javadocs
    BackendOptions getOptions();

}
