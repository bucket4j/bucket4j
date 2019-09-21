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
import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.AsyncBucket;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.GetConfigurationCommand;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * TODO javadocs
 * Represents an extension point of bucket4j library.
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

    // TODO javadocs
    <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command);

    /**
     * TODO
     *
     * Describes whether or not this backend supports asynchronous mode.
     *
     * <p>If asynchronous mode is  not supported any attempt to call {@link #asAsync()} will fail with {@link UnsupportedOperationException}
     *
     * @return true if this extension supports asynchronous mode.
     */
    boolean isAsyncModeSupported();

    // TODO javadocs
    <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command);

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

    /**
     * TODO
     *
     * Locates configuration of bucket which actually stored outside current JVM.
     *
     * @param key the unique identifier used to point to the bucket in external storage.
     *
     * @return Optional surround the configuration or empty optional if bucket with specified key are not stored.
     */
    default CompletableFuture<Optional<BucketConfiguration>> getProxyConfigurationAsync(K key) {
        if (!isAsyncModeSupported()) {
            throw BucketExceptions.asyncModeIsNotSupported();
        }
        GetConfigurationCommand cmd = new GetConfigurationCommand();
        return this.executeAsync(key, cmd).thenApply(result -> {
            if (result.isBucketNotFound()) {
                return Optional.empty();
            }
            return Optional.of(result.getData());
        });
    }

}
