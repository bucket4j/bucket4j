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

package io.github.bucket4j.grid.infinispan.hotrod;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.infinispan.client.hotrod.RemoteCache;

import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.grid.infinispan.Bucket4jInfinispan;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;

/**
 * The extension of Bucket4j library addressed to support <a href="https://infinispan.org/">Infinispan</a> in-memory computing platform via Hotrod client.
 */
public class HotrodInfinispanProxyManager<K> extends AbstractProxyManager<K> {

    private final RemoteCache<K, byte[]> remoteCache;

    public HotrodInfinispanProxyManager(Bucket4jInfinispan.HotrodInfinispanProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.remoteCache = builder.remoteCache;
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        byte[] requestBytes = InternalSerializationHelper.serializeRequest(request);
        Map<String, Object> params = new HashMap<>();
        params.put(Bucket4jTask.KEY_PARAM, key);
        params.put(Bucket4jTask.REQUEST_PARAM, requestBytes);
        byte[] responseBytes = remoteCache.execute(Bucket4jTask.TASK_NAME, params, key);
        return deserializeResult(responseBytes, request.getBackwardCompatibilityVersion());
    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }

    @Override
    public void removeProxy(K key) {
        remoteCache.remove(key);
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

}
