/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.github.bucket4j.grid.ignite;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Nothing;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridCommand;
import io.github.bucket4j.grid.GridProxy;
import io.github.bucket4j.grid.jcache.JCacheEntryProcessor;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteInClosure;

import javax.cache.processor.EntryProcessor;
import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

public class IgniteProxy<K extends Serializable> implements GridProxy<K> {

    private final IgniteCache<K, GridBucketState> cache;

    public IgniteProxy(IgniteCache<K, GridBucketState> cache) {
        this.cache = cache;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, GridCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.executeProcessor(command);
        return cache.invoke(key, adoptEntryProcessor(entryProcessor));
    }

    @Override
    public void createInitialState(K key, BucketConfiguration configuration) {
        JCacheEntryProcessor<K, Nothing> entryProcessor = JCacheEntryProcessor.initStateProcessor(configuration);
        cache.invoke(key, adoptEntryProcessor(entryProcessor));
    }

    @Override
    public <T extends Serializable> T createInitialStateAndExecute(K key, BucketConfiguration configuration, GridCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.initStateAndExecuteProcessor(command, configuration);
        CommandResult<T> result = cache.invoke(key, adoptEntryProcessor(entryProcessor));
        return result.getData();
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, GridCommand<T> command) throws UnsupportedOperationException {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.executeProcessor(command);
        return invokeAsync(key, entryProcessor);
    }

    @Override
    public <T extends Serializable> CompletableFuture<T> createInitialStateAndExecuteAsync(K key, BucketConfiguration configuration, GridCommand<T> command) throws UnsupportedOperationException {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.initStateAndExecuteProcessor(command, configuration);
        CompletableFuture<CommandResult<T>> result = invokeAsync(key, entryProcessor);
        return result.thenApply(f -> f.getData());
    }

    @Override
    public boolean isAsyncModeSupported() {
        // because JCache does not specify async API
        return true;
    }

    private <T> CacheEntryProcessor<K, GridBucketState, T> adoptEntryProcessor(final EntryProcessor<K, GridBucketState, T> entryProcessor) {
        return (CacheEntryProcessor<K, GridBucketState, T>) (entry, arguments) -> entryProcessor.process(entry, arguments);
    }

    private <T extends Serializable> CompletableFuture<CommandResult<T>> invokeAsync(K key, JCacheEntryProcessor<K, T> entryProcessor) {
        IgniteCache<K, GridBucketState> asyncCache = cache.withAsync();
        asyncCache.invoke(key, adoptEntryProcessor(entryProcessor));
        CompletableFuture<CommandResult<T>> completableFuture = new CompletableFuture<>();
        IgniteFuture<CommandResult<T>> igniteFuture = asyncCache.future();
        IgniteInClosure<? super IgniteFuture<CommandResult<T>>> listener = (IgniteInClosure<IgniteFuture<CommandResult<T>>>) completedIgniteFuture -> {
            Throwable exception = null;
            CommandResult<T> result = null;
            try {
                result = completedIgniteFuture.get();
            } catch (Throwable t) {
                exception = t;
            }
            if (exception != null) {
                completableFuture.completeExceptionally(exception);
            } else {
                completableFuture.complete(result);
            }
        };
        igniteFuture.listen(listener);
        return completableFuture;
    }

}
