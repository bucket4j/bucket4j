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

package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.IMap;
import com.hazelcast.map.EntryProcessor;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Nothing;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridCommand;
import io.github.bucket4j.grid.GridProxy;
import io.github.bucket4j.grid.jcache.JCacheEntryProcessor;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class HazelcastProxy<K extends Serializable> implements GridProxy<K> {

    private final IMap<K, GridBucketState> cache;

    public HazelcastProxy(IMap<K, GridBucketState> cache) {
        this.cache = cache;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, GridCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.executeProcessor(command);
        return (CommandResult<T>) cache.executeOnKey(key, adoptEntryProcessor(entryProcessor));
    }

    @Override
    public void createInitialState(K key, BucketConfiguration configuration) {
        JCacheEntryProcessor<K, Nothing> entryProcessor = JCacheEntryProcessor.initStateProcessor(configuration);
        cache.executeOnKey(key, adoptEntryProcessor(entryProcessor));
    }

    @Override
    public <T extends Serializable> T createInitialStateAndExecute(K key, BucketConfiguration configuration, GridCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.initStateAndExecuteProcessor(command, configuration);
        CommandResult<T> result = (CommandResult<T>) cache.executeOnKey(key, adoptEntryProcessor(entryProcessor));
        return result.getData();
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, GridCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.executeProcessor(command);
        return invokeAsync(key, entryProcessor);
    }

    @Override
    public <T extends Serializable> CompletableFuture<T> createInitialStateAndExecuteAsync(K key, BucketConfiguration configuration, GridCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.initStateAndExecuteProcessor(command, configuration);
        CompletableFuture<CommandResult<T>> result = invokeAsync(key, entryProcessor);
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

    private <T extends Serializable>  EntryProcessor adoptEntryProcessor(final JCacheEntryProcessor<K, T> entryProcessor) {
        return new HazelcastEntryProcessorAdapter<>(entryProcessor);
    }

    private <T extends Serializable> CompletableFuture<CommandResult<T>> invokeAsync(K key, JCacheEntryProcessor<K, T> entryProcessor) {
        CompletableFuture<CommandResult<T>> future = new CompletableFuture<>();
        cache.submitToKey(key, adoptEntryProcessor(entryProcessor), new ExecutionCallback() {
            @Override
            public void onResponse(Object response) {
                future.complete((CommandResult<T>) response);
            }

            @Override
            public void onFailure(Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

}
