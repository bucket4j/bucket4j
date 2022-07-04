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
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class RemoteStat implements ComparableByContent<RemoteStat> {

    private long consumedTokens;

    public RemoteStat(long consumedTokens) {
        this.consumedTokens = consumedTokens;
    }

    public long getConsumedTokens() {
        return consumedTokens;
    }

    public void addConsumedTokens(long consumedTokens) {
        this.consumedTokens += consumedTokens;
    }

    public static final SerializationHandle<RemoteStat> SERIALIZATION_HANDLE = new SerializationHandle<RemoteStat>() {
        @Override
        public <S> RemoteStat deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long consumedTokens = adapter.readLong(input);
            return new RemoteStat(consumedTokens);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, RemoteStat stat, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            adapter.writeLong(output, stat.consumedTokens);
        }

        @Override
        public int getTypeId() {
            return 6;
        }

        @Override
        public Class<RemoteStat> getSerializedType() {
            return RemoteStat.class;
        }

        @Override
        public RemoteStat fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long consumedTokens = readLongValue(snapshot, "consumedTokens");
            return new RemoteStat(consumedTokens);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(RemoteStat stat, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());
            result.put("consumedTokens", stat.consumedTokens);
            return result;
        }

        @Override
        public String getTypeName() {
            return "RemoteStat";
        }

    };

    public RemoteStat copy() {
        return new RemoteStat(consumedTokens);
    }

    @Override
    public boolean equalsByContent(RemoteStat other) {
        return consumedTokens == other.consumedTokens;
    }

}
