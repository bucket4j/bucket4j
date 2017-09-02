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

package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Nothing;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridCommand;
import io.github.bucket4j.grid.GridProxy;
import io.github.bucket4j.grid.jcache.JCacheEntryProcessor;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;

import javax.cache.Cache;
import java.io.Serializable;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class InfinispanProxy<K extends Serializable> implements GridProxy<K> {

    private final Cache<K, GridBucketState> cache;
    private final org.infinispan.Cache<K, GridBucketState> nativeCache;

    public InfinispanProxy(Cache<K, GridBucketState> cache) {
        this.cache = cache;
        this.nativeCache = cache.unwrap(org.infinispan.Cache.class);
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, GridCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.executeProcessor(command);
        return cache.invoke(key, entryProcessor);
    }

    @Override
    public void createInitialState(K key, BucketConfiguration configuration) {
        JCacheEntryProcessor<K, Nothing> entryProcessor = JCacheEntryProcessor.initStateProcessor(configuration);
        cache.invoke(key, entryProcessor);
    }

    @Override
    public <T extends Serializable> T createInitialStateAndExecute(K key, BucketConfiguration configuration, GridCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.initStateAndExecuteProcessor(command, configuration);
        CommandResult<T> result = cache.invoke(key, entryProcessor);
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
        return true;
    }

    private <T extends Serializable> CompletableFuture<CommandResult<T>> invokeAsync(final K key, final JCacheEntryProcessor<K, T> entryProcessor) {
        Address primaryNodeForKey = nativeCache.getAdvancedCache().getDistributionManager().getCacheTopology().getDistribution(key).primary();

        CompletableFuture<CommandResult<T>> resultFuture = new CompletableFuture<>();
        String cacheName = nativeCache.getName();

        Function<EmbeddedCacheManager, CommandResult<T>> callable = new SerializableRemoteFunction<>(key, cacheName, entryProcessor);

        nativeCache.getCacheManager().executor()
            .filterTargets(Collections.singleton(primaryNodeForKey))
            .submitConsumer(callable, (address, result, throwable) -> {
                if (throwable != null) {
                    resultFuture.completeExceptionally(throwable);
                } else {
                    resultFuture.complete(result);
                }
            });
        return resultFuture;
    }

}
