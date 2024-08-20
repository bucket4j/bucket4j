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

import java.util.concurrent.CompletableFuture;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteInClosure;

import io.github.bucket4j.distributed.proxy.AbstractAsyncProxyManager;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.versioning.Version;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;


/**
 * The extension of Bucket4j library addressed to support <a href="https://ignite.apache.org/">Apache ignite</a> in-memory computing platform.
 */
public class IgniteAsyncProxyManager<K> extends AbstractAsyncProxyManager<K> {

    private final IgniteCache<K, byte[]> cache;

    IgniteAsyncProxyManager(Bucket4jIgniteThick.IgniteAsyncProxyManagerBuilder<K> builder) {
        super(builder.getProxyManagerConfig());
        cache = builder.cache;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        IgniteEntryProcessor<K> entryProcessor = new IgniteEntryProcessor<>(request);
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

    @Override
    public boolean isExpireAfterWriteSupported() {
        return false;
    }

}
