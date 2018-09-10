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

package io.github.bucket4j.grid.jcache;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Nothing;
import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.RemoteCommand;
import io.github.bucket4j.remote.Backend;
import io.github.bucket4j.remote.RemoteBucketState;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.spi.CachingProvider;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class JCacheBackend<K extends Serializable> implements Backend<K> {

    private static final Map<String, String> incompatibleProviders = new HashMap<>();
    static {
        incompatibleProviders.put("org.infinispan", " use module bucket4j-infinispan directly");
    }

    private final Cache<K, RemoteBucketState> cache;

    public JCacheBackend(Cache<K, RemoteBucketState> cache) {
        this.cache = Objects.requireNonNull(cache);
        checkProviders(cache);
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.executeProcessor(command);
        return cache.invoke(key, entryProcessor);
    }

    @Override
    public void createInitialState(K key, BucketConfiguration configuration) {
        JCacheEntryProcessor<K, Nothing> entryProcessor = JCacheEntryProcessor.initStateProcessor(configuration);
        cache.invoke(key, entryProcessor);
    }

    @Override
    public <T extends Serializable> T createInitialStateAndExecute(K key, BucketConfiguration configuration, RemoteCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.initStateAndExecuteProcessor(command, configuration);
        CommandResult<T> result = cache.invoke(key, entryProcessor);
        return result.getData();
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        // because JCache does not specify async API
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Serializable> CompletableFuture<T> createInitialStateAndExecuteAsync(K key, BucketConfiguration configuration, RemoteCommand<T> command) {
        // because JCache does not specify async API
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<BucketConfiguration> getConfiguration(K key) {
        RemoteBucketState state = cache.get(key);
        if (state == null) {
            return Optional.empty();
        } else {
            return Optional.of(state.getConfiguration());
        }
    }

    @Override
    public boolean isAsyncModeSupported() {
        // because JCache does not specify async API
        return false;
    }

    private void checkProviders(Cache<K, RemoteBucketState> cache) {
        CacheManager cacheManager = cache.getCacheManager();
        if (cacheManager == null) {
            return;
        }

        CachingProvider cachingProvider = cacheManager.getCachingProvider();
        if (cachingProvider == null) {
            return;
        }

        String providerClassName = cachingProvider.getClass().getName();
        incompatibleProviders.forEach((providerPrefix, recommendation) -> {
            if (providerClassName.startsWith(providerPrefix)) {
                String message = "The Cache provider " + providerClassName + " is incompatible with Bucket4j, " + recommendation;
                throw new UnsupportedOperationException(message);
            }
        });
    }

}
