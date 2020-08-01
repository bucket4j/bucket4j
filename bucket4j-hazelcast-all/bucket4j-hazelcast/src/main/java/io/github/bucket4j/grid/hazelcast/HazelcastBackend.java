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
import com.hazelcast.map.IMap;
import io.github.bucket4j.distributed.proxy.AbstractBackend;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.grid.hazelcast.serialization.HazelcastEntryProcessorSerializer;
import io.github.bucket4j.grid.hazelcast.serialization.SimpleBackupProcessorSerializer;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;

/**
 * The extension of Bucket4j library addressed to support <a href="https://hazelcast.com//">Hazelcast</a> in-memory data grid.
 */
public class HazelcastBackend<K> extends AbstractBackend<K> {

    private final IMap<K, byte[]> map;

    public HazelcastBackend(IMap<K, byte[]> map) {
        this.map = Objects.requireNonNull(map);
    }

    @Override
    public <T> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        HazelcastEntryProcessor<K, T> entryProcessor = new HazelcastEntryProcessor<>(command);
        byte[] response = map.executeOnKey(key, entryProcessor);
        return deserializeResult(response);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        HazelcastEntryProcessor<K, T> entryProcessor = new HazelcastEntryProcessor<>(command);
        CompletionStage<byte[]> future = map.submitToKey(key, entryProcessor);
        return (CompletableFuture) future.thenApply(InternalSerializationHelper::deserializeResult);
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
