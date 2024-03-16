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

import io.github.bucket4j.distributed.proxy.ConfigurationNeedToBeReplacedException;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.github.bucket4j.distributed.versioning.Versions.v_8_1_0;

public class ConfigurationNeedToBeReplacedError implements CommandError, ComparableByContent<ConfigurationNeedToBeReplacedError> {

    private static final ConfigurationNeedToBeReplacedError INSTANCE = new ConfigurationNeedToBeReplacedError();

    @Override
    public RuntimeException asException() {
        return new ConfigurationNeedToBeReplacedException();
    }

    public static SerializationHandle<ConfigurationNeedToBeReplacedError> SERIALIZATION_HANDLE = new SerializationHandle<>() {
        @Override
        public <S> ConfigurationNeedToBeReplacedError deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_8_1_0, v_8_1_0);
            return INSTANCE;
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ConfigurationNeedToBeReplacedError error, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_8_1_0.getNumber());
        }

        @Override
        public int getTypeId() {
            return 40;
        }

        @Override
        public Class<ConfigurationNeedToBeReplacedError> getSerializedType() {
            return ConfigurationNeedToBeReplacedError.class;
        }

        @Override
        public ConfigurationNeedToBeReplacedError fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_8_1_0, v_8_1_0);
            return INSTANCE;
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(ConfigurationNeedToBeReplacedError error, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_8_1_0.getNumber());
            return result;
        }

        @Override
        public String getTypeName() {
            return "ConfigurationNeedToBeReplacedError";
        }

    };

    @Override
    public boolean equalsByContent(ConfigurationNeedToBeReplacedError other) {
        return true;
    }

}
