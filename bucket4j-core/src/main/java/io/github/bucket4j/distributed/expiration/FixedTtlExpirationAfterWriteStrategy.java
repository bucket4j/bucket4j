/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2024 Vladimir Bukhtoyarov
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
package io.github.bucket4j.distributed.expiration;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import static io.github.bucket4j.distributed.versioning.Versions.v_8_10_0;

public class FixedTtlExpirationAfterWriteStrategy implements ExpirationAfterWriteStrategy, ComparableByContent<FixedTtlExpirationAfterWriteStrategy> {
    private final long ttlMillis;

    public FixedTtlExpirationAfterWriteStrategy(Duration ttl) {
        long ttlMillis = ttl.toMillis();
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("ttl should be positive");
        }
        this.ttlMillis = ttlMillis;
    }

    @Override
    public long calculateTimeToLiveMillis(RemoteBucketState state, long currentTimeNanos) {
        return ttlMillis;
    }

    public static final SerializationHandle<FixedTtlExpirationAfterWriteStrategy> SERIALIZATION_HANDLE = new SerializationHandle<>() {
        @Override
        public <S> FixedTtlExpirationAfterWriteStrategy deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_8_10_0, v_8_10_0);

            long keepAfterRefillDurationMillis = adapter.readLong(input);
            return new FixedTtlExpirationAfterWriteStrategy(Duration.ofMillis(keepAfterRefillDurationMillis));
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, FixedTtlExpirationAfterWriteStrategy strategy, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_8_10_0.getNumber());

            adapter.writeLong(output, strategy.ttlMillis);
        }

        @Override
        public int getTypeId() {
            return 71;
        }

        @Override
        public Class<FixedTtlExpirationAfterWriteStrategy> getSerializedType() {
            return FixedTtlExpirationAfterWriteStrategy.class;
        }

        @Override
        public FixedTtlExpirationAfterWriteStrategy fromJsonCompatibleSnapshot(Map<String, Object> snapshot) {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_8_10_0, v_8_10_0);

            long keepAfterRefillDurationMillis = readLongValue(snapshot, "ttlMillis");
            return new FixedTtlExpirationAfterWriteStrategy(Duration.ofMillis(keepAfterRefillDurationMillis));
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(FixedTtlExpirationAfterWriteStrategy strategy, Version backwardCompatibilityVersion, Scope scope) {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_8_10_0.getNumber());
            result.put("ttlMillis", strategy.ttlMillis);
            return result;
        }

        @Override
        public String getTypeName() {
            return "FixedTtlExpirationAfterWriteStrategy";
        }

    };

    @Override
    public SerializationHandle<ExpirationAfterWriteStrategy> getSerializationHandle() {
        return (SerializationHandle) SERIALIZATION_HANDLE;
    }

    @Override
    public boolean equalsByContent(FixedTtlExpirationAfterWriteStrategy other) {
        return this.ttlMillis == other.ttlMillis;
    }

}
