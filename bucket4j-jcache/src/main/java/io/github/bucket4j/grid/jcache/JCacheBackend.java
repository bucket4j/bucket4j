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
import io.github.bucket4j.distributed.proxy.AbstractBackend;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.serialization.InternalSerializationHelper;

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
public class JCacheBackend<K> extends AbstractBackend<K> {

    private static final Map<String, String> incompatibleProviders = new HashMap<>();
    static {
        incompatibleProviders.put("org.infinispan", " use module bucket4j-infinispan directly");
    }

    private final Cache<K, byte[]> cache;

    public JCacheBackend(Cache<K, byte[]> cache) {
        this.cache = Objects.requireNonNull(cache);
        checkProviders(cache);
    }

    @Override
    public <T> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        BucketProcessor<K, T> entryProcessor = new BucketProcessor<>(command);
        return cache.invoke(key, entryProcessor);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        // because JCache does not specify async API
        throw new UnsupportedOperationException();
    }

    private void checkProviders(Cache<K, byte[]> cache) {
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

    private static class BucketProcessor<K, T> implements Serializable, EntryProcessor<K, byte[], CommandResult<T>> {

        private static final long serialVersionUID = 1;

        @Override
        public CommandResult<T> process(MutableEntry<K, byte[]> mutableEntry, Object... arguments) {
            byte[] serializedCommand = (byte[]) arguments[0];
            RemoteCommand<T> targetCommand;
            JCacheBucketEntry bucketEntry = new JCacheBucketEntry(mutableEntry);
            return targetCommand.execute(bucketEntry, TimeMeter.SYSTEM_MILLISECONDS.currentTimeNanos());
        }

    }


    private static class JCacheBucketEntry implements MutableBucketEntry {

        private final MutableEntry<?, byte[]> targetEntry;

        private JCacheBucketEntry(MutableEntry<?, byte[]> targetEntry) {
            this.targetEntry = targetEntry;
        }

        @Override
        public boolean exists() {
            return targetEntry.exists();
        }

        @Override
        public void set(RemoteBucketState state) {
            byte[] bytes = InternalSerializationHelper.serializeState(state);
            targetEntry.setValue(bytes);
        }

        @Override
        public RemoteBucketState get() {
            return targetEntry.getValue();
        }

    }

}
