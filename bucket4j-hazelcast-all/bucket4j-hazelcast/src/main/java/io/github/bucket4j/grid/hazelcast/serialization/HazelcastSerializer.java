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
package io.github.bucket4j.grid.hazelcast.serialization;

import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.hazelcast.nio.serialization.TypedStreamDeserializer;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class HazelcastSerializer<T> implements StreamSerializer<T>, TypedStreamDeserializer<T> {

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
        for (HazelcastSerializer serializer : HazelcastSerializer.getAllSerializers(typeIdBase)) {
            serializationConfig.addSerializerConfig(
                    new SerializerConfig()
                            .setImplementation(serializer)
                            .setTypeClass(serializer.getSerializableType())
            );
        }
    }

    /**
     * Returns the list custom Hazelcast serializers for all classes from Bucket4j library which can be transferred over network.
     * Each serializer will have different typeId, and this id will not be changed in the feature releases.
     *
     * <p>
     *     <strong>Note:</strong> it would be better to leave an empty space in the Ids in order to handle the extension of Bucket4j library when new classes can be added to library.
     *     For example if you called {@code getAllSerializers(10000)} then it would be reasonable to avoid registering your custom types in the interval 10000-10100.
     * </p>
     *
     * @param typeIdBase a starting number from for typeId sequence
     *
     * @return
     */
    public static List<HazelcastSerializer<?>> getAllSerializers(final int typeIdBase) {
        List<HazelcastSerializer<?>> serializers = new ArrayList<>();
        for (SerializationHandle<?> serializationHandle : Bucket4j.getSerializationHandles()) {
            serializers.add(new HazelcastSerializer<>(serializationHandle.getTypeId() + typeIdBase, serializationHandle));
        }

        return serializers;
    }

    private static HazelcastSerializationAdapter ADAPTER = new HazelcastSerializationAdapter();

    private final int typeId;
    private final SerializationHandle<T> serializationHandle;

    public HazelcastSerializer(int typeId, SerializationHandle<T> serializationHandle) {
        this.typeId = typeId;
        this.serializationHandle = serializationHandle;
    }

    public HazelcastSerializer(SerializationHandle<T> serializationHandle) {
        this.typeId = serializationHandle.getTypeId();
        this.serializationHandle = serializationHandle;
    }

    public HazelcastSerializer<T> withBaseTypeId(int baseTypeId) {
        return new HazelcastSerializer<>(typeId + baseTypeId, serializationHandle);
    }

    public Class<T> getSerializableType() {
        return serializationHandle.getSerializedType();
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void write(ObjectDataOutput out, T serializable) throws IOException {
        serializationHandle.serialize(ADAPTER, out, serializable);
    }

    @Override
    public T read(ObjectDataInput in) throws IOException {
        return read0(in);
    }

    @Override
    public T read(ObjectDataInput in, Class aClass) throws IOException {
        return read0(in);
    }

    private T read0(ObjectDataInput in) throws IOException {
        return serializationHandle.deserialize(ADAPTER, in);
    }

}
