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

import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.IMap;
import io.github.bucket4j.distributed.proxy.AbstractBackend;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.*;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * The extension of Bucket4j library addressed to support <a href="https://hazelcast.com//">Hazelcast</a> in-memory data grid.
 */
public class HazelcastBackend<K extends Serializable> extends AbstractBackend<K> {

    private final IMap<K, RemoteBucketState> cache;

    public HazelcastBackend(IMap<K, RemoteBucketState> cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        HazelcastEntryProcessor<K, T> entryProcessor = new HazelcastEntryProcessor<>(command);
        return (CommandResult<T>) cache.executeOnKey(key, entryProcessor);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        HazelcastEntryProcessor<K, T> entryProcessor = new HazelcastEntryProcessor<>(command);
        CompletableFuture<CommandResult<T>> future = new CompletableFuture<>();
        cache.submitToKey(key, entryProcessor, new ExecutionCallback() {
            @Override
            public void onResponse(Object response) {
                future.complete((CommandResult<T>) response);
            }

            @Override
            public void onFailure(Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }


}
