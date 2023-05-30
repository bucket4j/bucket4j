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

package io.github.bucket4j.grid.ignite.thin.compute;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.grid.ignite.thin.ThinClientUtils;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientCompute;
import org.apache.ignite.client.IgniteClientFuture;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;


/**
 * The extension of Bucket4j library addressed to support <a href="https://ignite.apache.org/">Apache ignite</a> in-memory computing platform.
 */
public class IgniteThinClientProxyManager<K> extends AbstractProxyManager<K> {

    private final ClientCache<K, byte[]> cache;
    private final ClientCompute clientCompute;

    public IgniteThinClientProxyManager(ClientCache<K, byte[]> cache, ClientCompute clientCompute) {
        this(cache, clientCompute, ClientSideConfig.getDefault());
    }

    public IgniteThinClientProxyManager(ClientCache<K, byte[]> cache, ClientCompute clientCompute, ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
        this.cache = Objects.requireNonNull(cache);
        this.clientCompute = Objects.requireNonNull(clientCompute);
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        IgniteEntryProcessor<K> entryProcessor = new IgniteEntryProcessor<>(request);
        Bucket4jComputeTaskParams<K> taskParams = new Bucket4jComputeTaskParams<>(cache.getName(), key, entryProcessor);
        try {
            byte[] resultBytes = clientCompute.execute(Bucket4jComputeTask.JOB_NAME, taskParams);
            return deserializeResult(resultBytes, request.getBackwardCompatibilityVersion());
        } catch (InterruptedException e) {
            throw BucketExceptions.executionException(e);
        }
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        IgniteEntryProcessor<K> entryProcessor = new IgniteEntryProcessor<>(request);
        Bucket4jComputeTaskParams<K> taskParams = new Bucket4jComputeTaskParams<>(cache.getName(), key, entryProcessor);

        IgniteClientFuture<byte[]> igniteFuture = clientCompute.executeAsync2(Bucket4jComputeTask.JOB_NAME, taskParams);
        CompletableFuture<byte[]> completableFuture = ThinClientUtils.convertFuture(igniteFuture);
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        return completableFuture.thenApply((byte[] resultBytes) -> deserializeResult(resultBytes, backwardCompatibilityVersion));
    }

    @Override
    public void removeProxy(K key) {
        cache.remove(key);
    }

    @Override
    protected CompletableFuture<?> removeAsync(K key) {
        IgniteClientFuture<Boolean> igniteFuture = cache.removeAsync(key);
        return ThinClientUtils.convertFuture(igniteFuture);
    }

}
