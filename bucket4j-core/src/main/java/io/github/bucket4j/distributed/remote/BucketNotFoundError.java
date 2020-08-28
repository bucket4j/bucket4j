package io.github.bucket4j.distributed.remote;

import io.github.bucket4j.distributed.proxy.BucketNotFoundException;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;

import static io.github.bucket4j.distributed.versioning.Versions.v_5_0_0;

public class BucketNotFoundError implements CommandError, ComparableByContent<BucketNotFoundError> {

    private static final BucketNotFoundError INSTANCE = new BucketNotFoundError();

    @Override
    public RuntimeException asException() {
        return new BucketNotFoundException();
    }

    public static SerializationHandle<BucketNotFoundError> SERIALIZATION_HANDLE = new SerializationHandle<BucketNotFoundError>() {
        @Override
        public <S> BucketNotFoundError deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_5_0_0, v_5_0_0);
            return INSTANCE;
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, BucketNotFoundError error, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeInt(output, v_5_0_0.getNumber());
        }

        @Override
        public int getTypeId() {
            return 15;
        }

        @Override
        public Class<BucketNotFoundError> getSerializedType() {
            return (Class) BucketNotFoundError.class;
        }

    };

    @Override
    public boolean equalsByContent(BucketNotFoundError other) {
        return true;
    }

}
