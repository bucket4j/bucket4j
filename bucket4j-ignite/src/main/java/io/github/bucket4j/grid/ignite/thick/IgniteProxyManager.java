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

package io.github.bucket4j.grid.ignite.thick;

import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.versioning.Version;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteInClosure;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.*;


/**
 * The extension of Bucket4j library addressed to support <a href="https://ignite.apache.org/">Apache ignite</a> in-memory computing platform.
 */
public class IgniteProxyManager<K> extends AbstractProxyManager<K> {

    private final IgniteCache<K, byte[]> cache;

    public IgniteProxyManager(IgniteCache<K, byte[]> cache) {
        this(cache, ClientSideConfig.getDefault());
    }

    public IgniteProxyManager(IgniteCache<K, byte[]> cache, ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
        this.cache = Objects.requireNonNull(cache);
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        IgniteProcessor<K> entryProcessor = new IgniteProcessor<>(request);
        byte[] resultBytes = cache.invoke(key, entryProcessor);
        return deserializeResult(resultBytes, request.getBackwardCompatibilityVersion());
    }

    @Override
    public void removeProxy(K key) {
        cache.remove(key);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        IgniteProcessor<K> entryProcessor = new IgniteProcessor<>(request);
        IgniteFuture<byte[]> igniteFuture = cache.invokeAsync(key, entryProcessor);
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        CompletableFuture<CommandResult<T>> completableFuture = new CompletableFuture<>();
        igniteFuture.listen((IgniteInClosure<IgniteFuture<byte[]>>) completedIgniteFuture -> {
            try {
                byte[] resultBytes = completedIgniteFuture.get();
                CommandResult<T> result = deserializeResult(resultBytes, backwardCompatibilityVersion);
                completableFuture.complete(result);
            } catch (Throwable t) {
                completableFuture.completeExceptionally(t);
            }
        });
        return completableFuture;
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        IgniteFuture<Boolean> igniteFuture = cache.removeAsync(key);
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        igniteFuture.listen((IgniteInClosure<IgniteFuture<Boolean>>) completedIgniteFuture -> {
            try {
                completedIgniteFuture.get();
                completableFuture.complete(null);
            } catch (Throwable t) {
                completableFuture.completeExceptionally(t);
            }
        });
        return completableFuture;
    }

    private static class IgniteProcessor<K> implements Serializable, CacheEntryProcessor<K, byte[], byte[]> {

        private static final long serialVersionUID = 1;

        private final byte[] requestBytes;

        private IgniteProcessor(Request<?> request) {
            this.requestBytes = serializeRequest(request);
        }

        @Override
        public byte[] process(MutableEntry<K, byte[]> entry, Object... arguments) throws EntryProcessorException {
            return new AbstractBinaryTransaction(requestBytes) {
                @Override
                public boolean exists() {
                    return entry.exists();
                }

                @Override
                protected byte[] getRawState() {
                    return entry.getValue();
                }

                @Override
                protected void setRawState(byte[] newStateBytes, RemoteBucketState newState) {
                    entry.setValue(newStateBytes);
                }
            }.execute();
        }

    }

}
