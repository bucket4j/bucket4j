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
package io.github.bucket4j.grid.ignite.thin;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.Nothing;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridCommand;
import io.github.bucket4j.grid.GridProxy;
import io.github.bucket4j.grid.jcache.JCacheEntryProcessor;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientCompute;
import org.apache.ignite.client.IgniteClientFuture;

public class IgniteClientProxy<K extends Serializable> implements GridProxy<K> {

    private final ClientCache<K, GridBucketState> cache;
    private final ClientCompute clientCompute;

    public IgniteClientProxy(ClientCompute clientCompute, ClientCache<K, GridBucketState> cache) {
        this.cache = cache;
        this.clientCompute = clientCompute;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, GridCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.executeProcessor(command);
        Bucket4jComputeTaskParams<K, T> taskParams = new Bucket4jComputeTaskParams<>(cache.getName(), key, entryProcessor);
        try {
            CommandResult<T> result = clientCompute.execute(Bucket4jComputeTask.JOB_NAME, taskParams);
            return result;
        } catch (InterruptedException e) {
            throw BucketExceptions.executionException(e);
        }
    }

    @Override
    public void createInitialState(K key, BucketConfiguration configuration) {
        JCacheEntryProcessor<K, Nothing> entryProcessor = JCacheEntryProcessor.initStateProcessor(configuration);
        Bucket4jComputeTaskParams<K, Nothing> taskParams = new Bucket4jComputeTaskParams<>(cache.getName(), key, entryProcessor);
        try {
            clientCompute.execute(Bucket4jComputeTask.JOB_NAME, taskParams);
        } catch (InterruptedException e) {
            throw BucketExceptions.executionException(e);
        }
    }

    @Override
    public <T extends Serializable> T createInitialStateAndExecute(K key, BucketConfiguration configuration, GridCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.initStateAndExecuteProcessor(command, configuration);
        Bucket4jComputeTaskParams<K, T> taskParams = new Bucket4jComputeTaskParams<>(cache.getName(), key, entryProcessor);
        try {
            CommandResult<T> result = clientCompute.execute(Bucket4jComputeTask.JOB_NAME, taskParams);
            return result.getData();
        } catch (InterruptedException e) {
            throw BucketExceptions.executionException(e);
        }
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, GridCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.executeProcessor(command);
        Bucket4jComputeTaskParams<K, T> taskParams = new Bucket4jComputeTaskParams<>(cache.getName(), key, entryProcessor);
        IgniteClientFuture<CommandResult<T>> igniteFuture = clientCompute.executeAsync2(Bucket4jComputeTask.JOB_NAME, taskParams);
        return convertFuture(igniteFuture);
    }

    @Override
    public <T extends Serializable> CompletableFuture<T> createInitialStateAndExecuteAsync(K key, BucketConfiguration configuration, GridCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.initStateAndExecuteProcessor(command, configuration);
        Bucket4jComputeTaskParams<K, T> taskParams = new Bucket4jComputeTaskParams<>(cache.getName(), key, entryProcessor);
        IgniteClientFuture<CommandResult<T>> igniteFuture = clientCompute.executeAsync2(Bucket4jComputeTask.JOB_NAME, taskParams);
        CompletableFuture<CommandResult<T>> result = convertFuture(igniteFuture);
        return result.thenApply(CommandResult::getData);
    }

    @Override
    public Optional<BucketConfiguration> getConfiguration(K key) {
        GridBucketState state = cache.get(key);
        if (state == null) {
            return Optional.empty();
        } else {
            return Optional.of(state.getConfiguration());
        }
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    private static <T> CompletableFuture<T> convertFuture(IgniteClientFuture<T> igniteFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        igniteFuture.whenComplete((T result, Throwable error) -> {
            if (error != null) {
                completableFuture.completeExceptionally(error);
            } else {
                completableFuture.complete(result);
            }
        });
        return completableFuture;
    }

}
