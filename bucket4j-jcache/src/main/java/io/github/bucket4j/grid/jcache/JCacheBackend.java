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

import io.github.bucket4j.*;
import io.github.bucket4j.remote.Backend;
import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.RemoteBucketState;
import io.github.bucket4j.remote.RemoteCommand;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.spi.CachingProvider;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * The extension of Bucket4j library addressed to support <a href="https://www.jcp.org/en/jsr/detail?id=107">JCache API (JSR 107)</a> specification.
 */
public class JCacheBackend<K extends Serializable> implements Backend<K> {

    private static final BucketOptions OPTIONS = new BucketOptions(false, MathType.ALL, MathType.INTEGER_64_BITS);

    private static final Map<String, String> incompatibleProviders = new HashMap<>();
    static {
        incompatibleProviders.put("org.infinispan", " use module bucket4j-infinispan directly");
    }

    private final Cache<K, RemoteBucketState> cache;
    private final TimeMeter clientClock;

    public JCacheBackend(Cache<K, RemoteBucketState> cache) {
        this.cache = Objects.requireNonNull(cache);
        checkProviders(cache);
        this.clientClock = null;
    }

    JCacheBackend(Cache<K, RemoteBucketState> cache, TimeMeter clientClock) {
        this.cache = Objects.requireNonNull(cache);
        checkProviders(cache);
        this.clientClock = Objects.requireNonNull(clientClock);
    }

    @Override
    public BucketOptions getOptions() {
        return OPTIONS;
    }

    @Override
    public TimeMeter getClientSideClock() {
        return clientClock;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.executeProcessor(command);
        return cache.invoke(key, entryProcessor);
    }

    @Override
    public void createInitialState(K key, BucketConfiguration configuration) {
        JCacheEntryProcessor<K, Nothing> entryProcessor = JCacheEntryProcessor.initStateProcessor(configuration, clientClock);
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
