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

package io.github.bucket4j.grid.coherence;


import com.tangosol.net.NamedCache;
import com.tangosol.util.processor.SingleEntryAsynchronousProcessor;
import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.versioning.Version;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.*;


/**
 * The extension of Bucket4j library addressed to support <a href="https://www.oracle.com/technetwork/middleware/coherence/overview/index.html">Oracle Coherence</a> in-memory computing platform.
 *
 * @param <K>
 */
public class CoherenceProxyManager<K> extends AbstractProxyManager<K> {

    private final NamedCache<K, byte[]> cache;

    public CoherenceProxyManager(NamedCache<K, byte[]> cache) {
        this(cache, ClientSideConfig.getDefault());
    }

    /**
     * @deprecated use {@link Bucket4jCoherence#builderFor(NamedCache)}
     */
    @Deprecated
    public CoherenceProxyManager(NamedCache<K, byte[]> cache, ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
        this.cache = cache;
    }

    CoherenceProxyManager(Bucket4jCoherence.CoherenceProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.cache = builder.cache;
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        CoherenceProcessor<K, T> entryProcessor = new CoherenceProcessor<>(request);
        byte[] resultBytes = cache.invoke(key, entryProcessor);
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        return deserializeResult(resultBytes, backwardCompatibilityVersion);
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
        CoherenceProcessor<K, T> entryProcessor = new CoherenceProcessor<>(request);
        CompletableFuture<CommandResult<T>> future = new CompletableFuture<>();
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        SingleEntryAsynchronousProcessor<K, byte[], byte[]> asyncProcessor =
            new SingleEntryAsynchronousProcessor<K, byte[], byte[]>(entryProcessor) {
                @Override
                public void onResult(Map.Entry<K, byte[]> entry) {
                    super.onResult(entry);
                    byte[] resultBytes = entry.getValue();
                    future.complete(deserializeResult(resultBytes, backwardCompatibilityVersion));
                }
                @Override
                public void onException(Throwable error) {
                    super.onException(error);
                    future.completeExceptionally(error);
                }
            };
        cache.invoke(key, asyncProcessor);
        return future;
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        return cache.async().remove(key).thenApply(oldState -> null);
    }

}
