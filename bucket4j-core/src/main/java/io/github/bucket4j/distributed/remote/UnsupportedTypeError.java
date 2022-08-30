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
package io.github.bucket4j.distributed.remote;

import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.UnsupportedTypeException;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class UnsupportedTypeError implements CommandError, ComparableByContent<UnsupportedTypeError> {

    private final int typeId;

    public UnsupportedTypeError(int typeId) {
        this.typeId = typeId;
    }

    public int getTypeId() {
        return typeId;
    }

    @Override
    public RuntimeException asException() {
        return new UnsupportedTypeException(typeId);
    }

    @Override
    public boolean equalsByContent(UnsupportedTypeError other) {
        return other.typeId == typeId;
    }

    public static SerializationHandle<UnsupportedTypeError> SERIALIZATION_HANDLE = new SerializationHandle<UnsupportedTypeError>() {
        @Override
        public <S> UnsupportedTypeError deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            int typeId = adapter.readInt(input);
            return new UnsupportedTypeError(typeId);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, UnsupportedTypeError error, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());
            adapter.writeInt(output, error.typeId);
        }

        @Override
        public int getTypeId() {
            return 16;
        }

        @Override
        public Class<UnsupportedTypeError> getSerializedType() {
            return (Class) UnsupportedTypeError.class;
        }

        @Override
        public UnsupportedTypeError fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);
            int typeId = readIntValue(snapshot, "typeId");
            return new UnsupportedTypeError(typeId);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(UnsupportedTypeError error, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());
            result.put("typeId", error.typeId);
            return result;
        }

        @Override
        public String getTypeName() {
            return "UnsupportedTypeError";
        }

    };

}
