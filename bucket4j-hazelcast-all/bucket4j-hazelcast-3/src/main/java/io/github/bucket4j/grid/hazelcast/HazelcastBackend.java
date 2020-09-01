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

import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.IMap;
import io.github.bucket4j.distributed.proxy.AbstractBackend;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.grid.hazelcast.serialization.HazelcastEntryProcessorSerializer;
import io.github.bucket4j.grid.hazelcast.serialization.SimpleBackupProcessorSerializer;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;

/**
 * The extension of Bucket4j library addressed to support <a href="https://hazelcast.com//">Hazelcast</a> in-memory data grid.
 */
public class HazelcastBackend<K> extends AbstractBackend<K> {

    private final IMap<K, byte[]> map;

    public HazelcastBackend(IMap<K, byte[]> map, ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
        this.map = Objects.requireNonNull(map);
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        HazelcastEntryProcessor<K, T> entryProcessor = new HazelcastEntryProcessor<>(request);
        byte[] response = (byte[]) map.executeOnKey(key, entryProcessor);
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        return deserializeResult(response, backwardCompatibilityVersion);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        HazelcastEntryProcessor<K, T> entryProcessor = new HazelcastEntryProcessor<>(request);
        CompletableFuture<CommandResult<T>> future = new CompletableFuture<>();
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        map.submitToKey(key, entryProcessor, new ExecutionCallback<byte[]>() {
            @Override
            public void onResponse(byte[] response) {
                try {
                    CommandResult<T> result = deserializeResult(response, backwardCompatibilityVersion);
                    future.complete(result);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Registers custom Hazelcast serializers for all classes from Bucket4j library which can be transferred over network.
     * Each serializer will have different typeId, and this id will not be changed in the feature releases.
     *
     * <p>
     *     <strong>Note:</strong> it would be better to leave an empty space in the Ids in order to handle the extension of Bucket4j library when new classes can be added to library.
     *     For example if you called {@code getAllSerializers(10000)} then it would be reasonable to avoid registering your custom types in the interval 10000-10100.
     * </p>
     *
     * @param typeIdBase a starting number from for typeId sequence
     */
    public static void addCustomSerializers(SerializationConfig serializationConfig, final int typeIdBase) {
        serializationConfig.addSerializerConfig(
                new SerializerConfig()
                        .setImplementation(new HazelcastEntryProcessorSerializer(typeIdBase))
                        .setTypeClass(HazelcastEntryProcessor.class)
        );

        serializationConfig.addSerializerConfig(
                new SerializerConfig()
                        .setImplementation(new SimpleBackupProcessorSerializer(typeIdBase + 1))
                        .setTypeClass(SimpleBackupProcessor.class)
        );
    }

}
