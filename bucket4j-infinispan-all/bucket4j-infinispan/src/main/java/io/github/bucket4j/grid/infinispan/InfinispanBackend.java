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

package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.distributed.proxy.AbstractBackend;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import org.infinispan.commons.CacheException;
import org.infinispan.functional.FunctionalMap.ReadWriteMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * The extension of Bucket4j library addressed to support <a href="https://infinispan.org/">Infinispan</a> in-memory computing platform.
 */
public class InfinispanBackend<K> extends AbstractBackend<K> {

    private final ReadWriteMap<K, byte[]> readWriteMap;

    // TODO javadocs
    public InfinispanBackend(ReadWriteMap<K, byte[]> readWriteMap) {
        this.readWriteMap = Objects.requireNonNull(readWriteMap);
    }

    @Override
    public <T> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        InfinispanProcessor<K, T> entryProcessor = new InfinispanProcessor<>(command);
        try {
            CompletableFuture<byte[]> resultFuture = readWriteMap.eval(key, entryProcessor);
            return (CommandResult<T>) resultFuture.thenApply(InternalSerializationHelper::deserializeResult).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CacheException(e);
        }
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        try {
            InfinispanProcessor<K, T> entryProcessor = new InfinispanProcessor<>(command);
            CompletableFuture<byte[]> resultFuture = readWriteMap.eval(key, entryProcessor);
            return resultFuture.thenApply(InternalSerializationHelper::deserializeResult);
        } catch (Throwable t) {
            CompletableFuture<CommandResult<T>> fail = new CompletableFuture<>();
            fail.completeExceptionally(t);
            return fail;
        }
    }


}
