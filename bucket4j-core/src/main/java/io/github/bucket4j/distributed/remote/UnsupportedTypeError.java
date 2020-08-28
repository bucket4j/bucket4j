package io.github.bucket4j.distributed.remote;

import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.UnsupportedTypeException;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;

import static io.github.bucket4j.distributed.versioning.Versions.v_5_0_0;

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
        public <S> UnsupportedTypeError deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_5_0_0, v_5_0_0);

            int typeId = adapter.readInt(input);
            return new UnsupportedTypeError(typeId);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, UnsupportedTypeError error, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeInt(output, v_5_0_0.getNumber());
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

    };

}
