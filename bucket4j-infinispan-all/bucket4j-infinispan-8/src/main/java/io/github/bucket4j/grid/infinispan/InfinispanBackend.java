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

package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AbstractBackend;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.distributed.versioning.Version;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.api.functional.EntryView.ReadWriteEntryView;
import org.infinispan.commons.api.functional.FunctionalMap.ReadWriteMap;
import org.infinispan.util.SerializableFunction;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.*;

/**
 * The extension of Bucket4j library addressed to support <a href="https://infinispan.org/">Infinispan</a> in-memory computing platform.
 */
public class InfinispanBackend<K> extends AbstractBackend<K> {

    private final ReadWriteMap<K, byte[]> readWriteMap;

    // TODO javadocs
    public InfinispanBackend(ReadWriteMap<K, byte[]> readWriteMap, ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
        this.readWriteMap = Objects.requireNonNull(readWriteMap);
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        InfinispanProcessor<K> entryProcessor = new InfinispanProcessor<>(request);
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        try {
            CompletableFuture<byte[]> future = readWriteMap.eval(key, entryProcessor);
            byte[] result = future.get();
            return deserializeResult(result, backwardCompatibilityVersion);
        } catch (InterruptedException | ExecutionException e) {
            throw new CacheException(e);
        }
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        InfinispanProcessor<K> entryProcessor = new InfinispanProcessor<>(request);
        CompletableFuture<byte[]> resultFuture = readWriteMap.eval(key, entryProcessor);
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        return resultFuture.thenApply(resultBytes -> deserializeResult(resultBytes, backwardCompatibilityVersion));
    }

    private static class InfinispanProcessor<K> implements SerializableFunction<ReadWriteEntryView<K, byte[]>, byte[]> {

        private static final long serialVersionUID = 42L;

        private final byte[] requestBytes;

        public InfinispanProcessor(Request<?> request) {
            this.requestBytes = InternalSerializationHelper.serializeRequest(request);
        }

        @Override
        public byte[] apply(ReadWriteEntryView<K, byte[]> entryView) {
            return new AbstractBinaryTransaction(requestBytes) {
                @Override
                public boolean exists() {
                    return entryView.find().isPresent();
                }

                @Override
                protected byte[] getRawState() {
                    return entryView.get();
                }

                @Override
                protected void setRawState(byte[] stateBytes) {
                    entryView.set(stateBytes);
                }
            }.execute();
        }

    }

}
