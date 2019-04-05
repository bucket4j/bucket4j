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
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.remote.*;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;
import javax.cache.spi.CachingProvider;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * The extension of Bucket4j library addressed to support <a href="https://www.jcp.org/en/jsr/detail?id=107">JCache API (JSR 107)</a> specification.
 */
public class JCacheBackend<K extends Serializable> implements Backend<K> {

    private static final BackendOptions OPTIONS = new BackendOptions(false, MathType.ALL, MathType.INTEGER_64_BITS);

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
    public BackendOptions getOptions() {
        return OPTIONS;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        BucketProcessor<K, T> entryProcessor = new BucketProcessor<>(command);
        return cache.invoke(key, entryProcessor);
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        // because JCache does not specify async API
        throw new UnsupportedOperationException();
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

    private static class BucketProcessor<K extends Serializable, T extends Serializable> implements Serializable, EntryProcessor<K, RemoteBucketState, CommandResult<T>> {

        private static final long serialVersionUID = 1;

        private RemoteCommand<T> targetCommand;

        public BucketProcessor(RemoteCommand<T> targetCommand) {
            this.targetCommand = targetCommand;
        }

        @Override
        public CommandResult<T> process(MutableEntry<K, RemoteBucketState> mutableEntry, Object... arguments) {
            JCacheBucketEntry bucketEntry = new JCacheBucketEntry(mutableEntry);
            return targetCommand.execute(bucketEntry, TimeMeter.SYSTEM_MILLISECONDS.currentTimeNanos());
        }

    }


    private static class JCacheBucketEntry implements MutableBucketEntry {

        private final MutableEntry<?, RemoteBucketState> targetEntry;

        private JCacheBucketEntry(MutableEntry<?, RemoteBucketState> targetEntry) {
            this.targetEntry = targetEntry;
        }

        @Override
        public boolean exists() {
            return targetEntry.exists();
        }

        @Override
        public void set(RemoteBucketState state) {
            targetEntry.setValue(state);
        }

        @Override
        public RemoteBucketState get() {
            return targetEntry.getValue();
        }

    }

}
