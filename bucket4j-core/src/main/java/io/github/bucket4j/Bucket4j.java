/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j;

import io.github.bucket4j.distributed.proxy.AsyncBucketProxy;
import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.proxy.BucketProxy;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.local.LocalBucketBuilder;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * This is entry point for functionality provided bucket4j library.
 */
public class Bucket4j {

    private Bucket4j() {
        // to avoid initialization of utility class
    }

    /**
     * Creates the new builder of in-memory buckets.
     *
     * @return new instance of {@link LocalBucketBuilder}
     */
    public static LocalBucketBuilder builder() {
        return new LocalBucketBuilder();
    }

    /**
     * Creates new instance of {@link ConfigurationBuilder}
     *
     * @return instance of {@link ConfigurationBuilder}
     */
    public static ConfigurationBuilder configurationBuilder() {
        return new ConfigurationBuilder();
    }

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
    public static <K extends Serializable> BucketProxy onShotProxy(K key, Backend<K> backend, Supplier<BucketConfiguration> configurationSupplier) {
        CommandExecutor<K> commandExecutor = CommandExecutor.nonOptimized(backend);
        return BucketProxy.createLazyBucket(key, configurationSupplier, commandExecutor);
    }

    public static <K extends Serializable> BucketProxy onShotProxy(K key, Backend<K> backend, BucketConfiguration configuration) {
        // TODO fix javadocs
        throw new UnsupportedOperationException();
    }

    public static <K extends Serializable> AsyncBucketProxy onShotAsyncProxy(K key, Backend<K> backend, BucketConfiguration configuration) {
        // TODO fix javadocs
        throw new UnsupportedOperationException();
    }

    public static <K extends Serializable> AsyncBucketProxy onShotAsyncProxy(K key, Backend<K> backend, Supplier<CompletableFuture<BucketConfiguration>> asyncConfigurationSupplier) {
        // TODO fix javadocs
        throw new UnsupportedOperationException();
    }

}

