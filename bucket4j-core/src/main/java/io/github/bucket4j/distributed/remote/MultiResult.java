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

import io.github.bucket4j.distributed.serialization.*;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class MultiResult implements ComparableByContent<MultiResult> {

    private final List<CommandResult<?>> results;

    public static SerializationHandle<MultiResult> SERIALIZATION_HANDLE = new SerializationHandle<>() {
        @Override
        public <S> MultiResult deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            int size = adapter.readInt(input);
            List<CommandResult<?>> results = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                CommandResult<?> result = CommandResult.SERIALIZATION_HANDLE.deserialize(adapter, input);
                results.add(result);
            }
            return new MultiResult(results);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, MultiResult multiResult, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            adapter.writeInt(output, multiResult.results.size());
            for (CommandResult<?> result : multiResult.results) {
                CommandResult.SERIALIZATION_HANDLE.serialize(adapter, output, result, backwardCompatibilityVersion, scope);
            }
        }

        @Override
        public int getTypeId() {
            return 13;
        }

        @Override
        public Class<MultiResult> getSerializedType() {
            return MultiResult.class;
        }

        @Override
        public MultiResult fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            List<Map<String, Object>> resultSnapshots = (List<Map<String, Object>>) snapshot.get("results");
            List<CommandResult<?>> results = new ArrayList<>(resultSnapshots.size());
            for (Map<String, Object> resultSnapshot : resultSnapshots) {
                CommandResult<?> result = CommandResult.SERIALIZATION_HANDLE.fromJsonCompatibleSnapshot(resultSnapshot);
                results.add(result);
            }
            return new MultiResult(results);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(MultiResult multiResult, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("version", v_7_0_0.getNumber());

            List<Map<String, Object>> resultSnapshots = new ArrayList<>(multiResult.results.size());
            for (CommandResult<?> result : multiResult.results) {
                Map<String, Object> resultSnapshot = CommandResult.SERIALIZATION_HANDLE.toJsonCompatibleSnapshot(result, backwardCompatibilityVersion, scope);
                resultSnapshots.add(resultSnapshot);
            }

            snapshot.put("results", resultSnapshots);
            return snapshot;
        }

        @Override
        public String getTypeName() {
            return "MultiResult";
        }

    };

    public MultiResult(List<CommandResult<?>> results) {
        this.results = results;
    }

    public List<CommandResult<?>> getResults() {
        return results;
    }

    @Override
    public boolean equalsByContent(MultiResult other) {
        if (results.size() != other.results.size()) {
            return false;
        }
        for (int i = 0; i < results.size(); i++) {
            CommandResult<?> result1 = results.get(i);
            CommandResult<?> result2 = other.results.get(i);
            if (!ComparableByContent.equals(result1, result2)) {
                return false;
            }
        }
        return true;
    }

}
