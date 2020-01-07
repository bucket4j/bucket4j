package io.github.bucket4j.serialization;

import io.github.bucket4j.Nothing;

import java.io.IOException;

public class PrimitiveSerializationHandles {

    public static SerializationHandle<Nothing> NULL_HANDLE = new SerializationHandle<Nothing>() {

        @Override
        public <I> Nothing deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            return null;
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, Nothing serializableObject) throws IOException {

        }

        @Override
        public int getTypeId() {
            return 0;
        }

        @Override
        public Class<Nothing> getSerializedType() {
            return Nothing.class;
        }
    };

    public static SerializationHandle<Long> LONG_HANDLE = new SerializationHandle<Long>() {
        @Override
        public <I> Long deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            return adapter.readLong(input);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, Long value) throws IOException {
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
    };

    public static SerializationHandle<Boolean> BOOLEAN_HANDLE = new SerializationHandle<Boolean>() {
        @Override
        public <I> Boolean deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            return adapter.readBoolean(input);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, Boolean value) throws IOException {
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
    };

    static final SerializationHandle[] primitiveHandlesById = new SerializationHandle[] {
            NULL_HANDLE,
            LONG_HANDLE,
            BOOLEAN_HANDLE
    };

}
