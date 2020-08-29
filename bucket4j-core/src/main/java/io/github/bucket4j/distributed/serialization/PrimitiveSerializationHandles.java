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
package io.github.bucket4j.distributed.serialization;

import io.github.bucket4j.Nothing;
import io.github.bucket4j.distributed.versioning.Version;

import java.io.IOException;

public class PrimitiveSerializationHandles {

    public static SerializationHandle<Nothing> NULL_HANDLE = new SerializationHandle<Nothing>() {

        @Override
        public <I> Nothing deserialize(DeserializationAdapter<I> adapter, I input, Version backwardCompatibilityVersion) throws IOException {
            return null;
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, Nothing serializableObject, Version backwardCompatibilityVersion) throws IOException {

        }

        @Override
        public int getTypeId() {
            return 0;
        }

        @Override
        public Class<Nothing> getSerializedType() {
            return Nothing.class;
        }
    };

    public static SerializationHandle<Long> LONG_HANDLE = new SerializationHandle<Long>() {
        @Override
        public <I> Long deserialize(DeserializationAdapter<I> adapter, I input, Version backwardCompatibilityVersion) throws IOException {
            return adapter.readLong(input);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, Long value, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeLong(output, value);
        }

        @Override
        public int getTypeId() {
            return -1;
        }

        @Override
        public Class<Long> getSerializedType() {
            return Long.TYPE;
        }
    };

    public static SerializationHandle<Boolean> BOOLEAN_HANDLE = new SerializationHandle<Boolean>() {
        @Override
        public <I> Boolean deserialize(DeserializationAdapter<I> adapter, I input, Version backwardCompatibilityVersion) throws IOException {
            return adapter.readBoolean(input);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, Boolean value, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeBoolean(output, value);
        }

        @Override
        public int getTypeId() {
            return -2;
        }

        @Override
        public Class<Boolean> getSerializedType() {
            return Boolean.TYPE;
        }
    };

    public static final SerializationHandle[] primitiveHandlesById = new SerializationHandle[] {
            NULL_HANDLE,
            LONG_HANDLE,
            BOOLEAN_HANDLE
    };

}
