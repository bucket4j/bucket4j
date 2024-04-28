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
import io.github.bucket4j.distributed.versioning.UsageOfObsoleteApiException;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class UsageOfObsoleteApiError implements ComparableByContent<UsageOfObsoleteApiError>, CommandError {

    private final int requestedFormatNumber;
    private final int minSupportedFormatNumber;

    public UsageOfObsoleteApiError(int requestedFormatNumber, int minSupportedFormatNumber) {
        this.requestedFormatNumber = requestedFormatNumber;
        this.minSupportedFormatNumber = minSupportedFormatNumber;
    }

    public int getRequestedFormatNumber() {
        return requestedFormatNumber;
    }

    public int getMinSupportedFormatNumber() {
        return minSupportedFormatNumber;
    }

    @Override
    public RuntimeException asException() {
        return new UsageOfObsoleteApiException(requestedFormatNumber, minSupportedFormatNumber);
    }

    @Override
    public boolean equalsByContent(UsageOfObsoleteApiError other) {
        return other.requestedFormatNumber == requestedFormatNumber
                && other.minSupportedFormatNumber == minSupportedFormatNumber;
    }

    public static SerializationHandle<UsageOfObsoleteApiError> SERIALIZATION_HANDLE = new SerializationHandle<>() {
        @Override
        public <S> UsageOfObsoleteApiError deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            int requestedFormatNumber = adapter.readInt(input);
            int minSupportedFormatNumber = adapter.readInt(input);
            return new UsageOfObsoleteApiError(requestedFormatNumber, minSupportedFormatNumber);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, UsageOfObsoleteApiError error, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());
            adapter.writeInt(output, error.requestedFormatNumber);
            adapter.writeInt(output, error.minSupportedFormatNumber);
        }

        @Override
        public int getTypeId() {
            return 17;
        }

        @Override
        public Class<UsageOfObsoleteApiError> getSerializedType() {
            return UsageOfObsoleteApiError.class;
        }

        @Override
        public UsageOfObsoleteApiError fromJsonCompatibleSnapshot(Map<String, Object> snapshot) {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            int requestedFormatNumber = readIntValue(snapshot, "requestedFormatNumber");
            int minSupportedFormatNumber = readIntValue(snapshot, "minSupportedFormatNumber");

            return new UsageOfObsoleteApiError(requestedFormatNumber, minSupportedFormatNumber);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(UsageOfObsoleteApiError error, Version backwardCompatibilityVersion, Scope scope) {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());
            result.put("requestedFormatNumber", error.requestedFormatNumber);
            result.put("minSupportedFormatNumber", error.minSupportedFormatNumber);
            return result;
        }

        @Override
        public String getTypeName() {
            return "UsageOfObsoleteApiError";
        }

    };


}
