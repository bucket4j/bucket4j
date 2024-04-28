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
import java.util.HashMap;
import java.util.Map;

public class PrimitiveSerializationHandles {

    public static final SerializationHandle<Nothing> NULL_HANDLE = new SerializationHandle<>() {

        @Override
        public <I> Nothing deserialize(DeserializationAdapter<I> adapter, I input) {
            return null;
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, Nothing serializableObject, Version backwardCompatibilityVersion, Scope scope) {

        }

        @Override
        public int getTypeId() {
            return 0;
        }

        @Override
        public Class<Nothing> getSerializedType() {
            return Nothing.class;
        }

        @Override
        public Nothing fromJsonCompatibleSnapshot(Map<String, Object> snapshot) {
            return Nothing.INSTANCE;
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(Nothing serializableObject, Version backwardCompatibilityVersion, Scope scope) {
            return new HashMap<>();
        }

        @Override
        public String getTypeName() {
            return "Nothing";
        }
    };

    public static final SerializationHandle<Long> LONG_HANDLE = new SerializationHandle<>() {
        @Override
        public <I> Long deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            return adapter.readLong(input);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, Long value, Version backwardCompatibilityVersion, Scope scope) throws IOException {
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

        @Override
        public Long fromJsonCompatibleSnapshot(Map<String, Object> snapshot) {
            return readLongValue(snapshot, "value");
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(Long serializableObject, Version backwardCompatibilityVersion, Scope scope) {
            Map<String, Object> result = new HashMap<>();
            result.put("value", serializableObject);
            return result;
        }

        @Override
        public String getTypeName() {
            return "Long";
        }
    };

    public static final SerializationHandle<Boolean> BOOLEAN_HANDLE = new SerializationHandle<>() {
        @Override
        public <I> Boolean deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            return adapter.readBoolean(input);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, Boolean value, Version backwardCompatibilityVersion, Scope scope) throws IOException {
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

        @Override
        public Boolean fromJsonCompatibleSnapshot(Map<String, Object> snapshot) {
            return (Boolean) snapshot.get("value");
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(Boolean serializableObject, Version backwardCompatibilityVersion, Scope scope) {
            Map<String, Object> result = new HashMap<>();
            result.put("value", serializableObject);
            return result;
        }

        @Override
        public String getTypeName() {
            return "Boolean";
        }
    };

    public static final SerializationHandle[] primitiveHandlesById = new SerializationHandle[] {
            NULL_HANDLE,
            LONG_HANDLE,
            BOOLEAN_HANDLE
    };

}
