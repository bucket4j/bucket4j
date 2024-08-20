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

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;
import javax.cache.spi.CachingProvider;

import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.remote.AbstractBinaryTransaction;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;

/**
 * The extension of Bucket4j library addressed to support <a href="https://www.jcp.org/en/jsr/detail?id=107">JCache API (JSR 107)</a> specification.
 */
public class JCacheProxyManager<K> extends AbstractProxyManager<K> {

    private static final Map<String, String> incompatibleProviders;
    static {
        incompatibleProviders = Collections.emptyMap();
        // incompatibleProviders.put("org.infinispan", " use module bucket4j-infinispan directly");
    }

    private static final Set<String> preferLambdaStyleProviders = Collections.singleton("org.infinispan");

    private final Cache<K, byte[]> cache;
    private final boolean preferLambdaStyle;

    public JCacheProxyManager(Bucket4jJCache.JCacheProxyManagerBuilder<K> builder) {
        super(builder.getProxyManagerConfig());
        cache = builder.cache;
        checkCompatibilityWithProvider(cache);
        this.preferLambdaStyle = preferLambdaStyle(cache);
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        EntryProcessor<K, byte[], byte[]> entryProcessor = preferLambdaStyle? createLambdaProcessor(request) : new BucketProcessor<>(request);
        byte[] resultBytes = cache.invoke(key, entryProcessor);
        return InternalSerializationHelper.deserializeResult(resultBytes, request.getBackwardCompatibilityVersion());
    }

    @Override
    public void removeProxy(K key) {
        cache.remove(key);
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return false;
    }

    private void checkCompatibilityWithProvider(Cache<K, byte[]> cache) {
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

    private boolean preferLambdaStyle(Cache<K, byte[]> cache) {
        CacheManager cacheManager = cache.getCacheManager();
        if (cacheManager == null) {
            return false;
        }

        CachingProvider cachingProvider = cacheManager.getCachingProvider();
        if (cachingProvider == null) {
            return false;
        }

        String providerClassName = cachingProvider.getClass().getName();
        for (String providerPrefix : preferLambdaStyleProviders) {
            if (providerClassName.startsWith(providerPrefix)) {
                return true;
            }
        }

        return false;
    }

    public <T> EntryProcessor<K, byte[], byte[]> createLambdaProcessor(Request<T> request) {
        byte[] serializedRequest = InternalSerializationHelper.serializeRequest(request);
        return  (Serializable & EntryProcessor<K, byte[], byte[]>) (mutableEntry, objects)
                -> new JCacheTransaction(mutableEntry, serializedRequest).execute();
    }

    private static class BucketProcessor<K, T> implements Serializable, EntryProcessor<K, byte[], byte[]> {

        @Serial
        private static final long serialVersionUID = 911;

        private final byte[] serializedRequest;

        public BucketProcessor(Request<T> request) {
            this.serializedRequest = InternalSerializationHelper.serializeRequest(request);
        }

        @Override
        public byte[] process(MutableEntry<K, byte[]> mutableEntry, Object... arguments) {
            return new JCacheTransaction(mutableEntry, serializedRequest).execute();
        }

    }

    private static class JCacheTransaction extends AbstractBinaryTransaction {

        private final MutableEntry<?, byte[]> targetEntry;

        private JCacheTransaction(MutableEntry<?, byte[]> targetEntry, byte[] requestBytes) {
            super(requestBytes);
            this.targetEntry = targetEntry;
        }

        @Override
        public boolean exists() {
            return targetEntry.exists();
        }

        @Override
        protected byte[] getRawState() {
            return targetEntry.getValue();
        }

        @Override
        protected void setRawState(byte[] newStateBytes, RemoteBucketState newState) {
            targetEntry.setValue(newStateBytes);
        }

    }

}
