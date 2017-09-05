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

package io.github.bucket4j.grid.jcache;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Nothing;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridCommand;
import io.github.bucket4j.grid.GridProxy;
import io.github.bucket4j.grid.GridBucketState;

import javax.cache.Cache;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class JCacheProxy<K extends Serializable> implements GridProxy<K> {

    public static Map<String, String> incompatibleProviders = new HashMap<>();
    static {
        incompatibleProviders.put("org.infinispan", "bucket4j-infinispan");
    }

    private final Cache<K, GridBucketState> cache;

    public JCacheProxy(Cache<K, GridBucketState> cache) {
        this.cache = Objects.requireNonNull(cache);
        checkProviders(cache);
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
        // because JCache does not specify async API
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Serializable> CompletableFuture<T> createInitialStateAndExecuteAsync(K key, BucketConfiguration configuration, GridCommand<T> command) throws UnsupportedOperationException {
        // because JCache does not specify async API
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncModeSupported() {
        // because JCache does not specify async API
        return false;
    }

    private void checkProviders(Cache<K, GridBucketState> cache) {
        String providerClassName = cache.getCacheManager().getCachingProvider().getClass().getName();
        for (String prefix : incompatibleProviders.keySet()) {
            if (providerClassName.startsWith(prefix)) {
                String message = "The Cache provider " + providerClassName + " is incompatible with Bucket4j " +
                        " use module " + incompatibleProviders.get(prefix) + " directly";
                throw new UnsupportedOperationException(message);
            }
        }
    }

}
