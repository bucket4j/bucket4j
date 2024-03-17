package io.github.bucket4j.distributed.expiration;

import java.io.IOException;
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

public class NoneExpirationAfterWriteStrategy implements ExpirationAfterWriteStrategy, ComparableByContent<NoneExpirationAfterWriteStrategy> {
    public static final NoneExpirationAfterWriteStrategy INSTANCE = new NoneExpirationAfterWriteStrategy();

    @Override
    public long calculateTimeToLiveMillis(RemoteBucketState state, long currentTimeNanos) {
        return -1;
    }

    public static final SerializationHandle<NoneExpirationAfterWriteStrategy> SERIALIZATION_HANDLE = new SerializationHandle<>() {
        @Override
        public <S> NoneExpirationAfterWriteStrategy deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_8_10_0, v_8_10_0);
            return NoneExpirationAfterWriteStrategy.INSTANCE;
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, NoneExpirationAfterWriteStrategy strategy, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_8_10_0.getNumber());
        }

        @Override
        public int getTypeId() {
            return 72;
        }

        @Override
        public Class<NoneExpirationAfterWriteStrategy> getSerializedType() {
            return NoneExpirationAfterWriteStrategy.class;
        }

        @Override
        public NoneExpirationAfterWriteStrategy fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_8_10_0, v_8_10_0);
            return NoneExpirationAfterWriteStrategy.INSTANCE;
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(NoneExpirationAfterWriteStrategy strategy, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_8_10_0.getNumber());
            return result;
        }

        @Override
        public String getTypeName() {
            return "NoneExpirationAfterWriteStrategy";
        }

    };

    @Override
    public SerializationHandle<ExpirationAfterWriteStrategy> getSerializationHandle() {
        return (SerializationHandle) SERIALIZATION_HANDLE;
    }

    @Override
    public boolean equalsByContent(NoneExpirationAfterWriteStrategy other) {
        return true;
    }
}
