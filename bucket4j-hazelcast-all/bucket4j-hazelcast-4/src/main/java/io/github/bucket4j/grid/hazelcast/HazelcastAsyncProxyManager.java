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

package io.github.bucket4j.grid.hazelcast;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.hazelcast.map.IMap;

import io.github.bucket4j.distributed.proxy.AbstractAsyncProxyManager;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.grid.hazelcast.Bucket4jHazelcast.HazelcastAsyncProxyManagerBuilder;

/**
 * The extension of Bucket4j library addressed to support <a href="https://hazelcast.com//">Hazelcast</a> in-memory data grid.
 */
public class HazelcastAsyncProxyManager<K> extends AbstractAsyncProxyManager<K> {

    private final IMap<K, byte[]> map;
    private final String offloadableExecutorName;

    HazelcastAsyncProxyManager(HazelcastAsyncProxyManagerBuilder<K> builder) {
        super(builder.getProxyManagerConfig());
        this.map = builder.map;
        this.offloadableExecutorName = builder.offloadableExecutorName;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        HazelcastEntryProcessor<K, T> entryProcessor = offloadableExecutorName == null?
                new HazelcastEntryProcessor<>(request) :
                new HazelcastOffloadableEntryProcessor<>(request, offloadableExecutorName);
        CompletionStage<byte[]> future = map.submitToKey(key, entryProcessor);
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        return (CompletableFuture) future.thenApply((byte[] bytes) -> InternalSerializationHelper.deserializeResult(bytes, backwardCompatibilityVersion));
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        CompletionStage<byte[]> hazelcastFuture = map.removeAsync(key);
        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        hazelcastFuture.whenComplete((oldState, error) -> {
          if (error == null) {
              resultFuture.complete(null);
          } else {
              resultFuture.completeExceptionally(error);
          }
        });
        return resultFuture;
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }
}
