package io.github.bucket4j.grid.infinispan.serialization;

import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.AbstractMarshaller;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Bucket4jMarshaller extends AbstractMarshaller {

    private static final Map<Integer, SerializationHandle<?>> serializerById = new HashMap<>();
    private static final Map<Class<?>, SerializationHandle<?>> serializerByClass = new HashMap<>();
    private static final Set<Class<?>> supportedTypes = new HashSet<>();

    private static final int LONG_TYPE_ID = -1;
    private static final int BOOLEAN_TYPE_ID = -2;
    private static final int NULL_TYPE_ID = -3;

    static {
        for (SerializationHandle<?> serializer : Bucket4j.getSerializationHandles()) {
            serializerById.put(serializer.getTypeId(), serializer);
            serializerByClass.put(serializer.getSerializedType(), serializer);
            supportedTypes.add(serializer.getSerializedType());
        }
    }

    private static InfinispanSerializationAdapter ADAPTER = new InfinispanSerializationAdapter();

    public static class InfinispanSerializationAdapter implements SerializationAdapter<DataOutput>, DeserializationAdapter<DataInput> {

        @Override
        public boolean readBoolean(DataInput source) throws IOException {
            return source.readBoolean();
        }

        @Override
        public byte readByte(DataInput source) throws IOException {
            return source.readByte();
        }

        @Override
        public int readInt(DataInput source) throws IOException {
            return source.readInt();
        }

        @Override
        public long readLong(DataInput source) throws IOException {
            return source.readLong();
        }

        @Override
        public long[] readLongArray(DataInput source) throws IOException {
            int size = source.readInt();
            long array[] = new long[size];
            for (int i = 0; i < size; i++) {
                array[i] = source.readLong();
            }
            return array;
        }

        @Override
        public <T> T readObject(DataInput source, Class<T> type) throws IOException {
            return (T) readObject(source);
        }

        @Override
        public Object readObject(DataInput source) throws IOException {
            int typeId = source.readInt();
            if (typeId > 0) {
                SerializationHandle serializer = serializerById.get(typeId);
                if (serializer == null) {
                    throw new IllegalStateException("Unknown type id " + typeId);
                }
                return serializer.deserialize(this, source);
            } else {
                switch (typeId) {
                    case BOOLEAN_TYPE_ID: return source.readBoolean();
                    case LONG_TYPE_ID: return source.readLong();
                    case NULL_TYPE_ID: return null;
                    default: throw new IllegalStateException("Unknown type id " + typeId);
                }
            }
        }

        @Override
        public void writeBoolean(DataOutput target, boolean value) throws IOException {
            target.writeBoolean(value);
        }

        @Override
        public void writeByte(DataOutput target, byte value) throws IOException {
            target.writeByte(value);
        }

        @Override
        public void writeInt(DataOutput target, int value) throws IOException {
            target.writeInt(value);
        }

        @Override
        public void writeLong(DataOutput target, long value) throws IOException {
            target.writeLong(value);
        }

        @Override
        public void writeLongArray(DataOutput target, long[] value) throws IOException {
            target.writeInt(value.length);
            for (int i = 0; i < value.length; i++) {
                target.writeLong(value[i]);
            }
        }

        @Override
        public void writeObject(DataOutput target, Object value) throws IOException {
            if (value == null) {
                target.writeInt(NULL_TYPE_ID);
                return;
            }
            SerializationHandle serializer = serializerByClass.get(value.getClass());
            if (serializer != null) {
                target.writeInt(serializer.getTypeId());
                serializer.serialize(this, target, value);
            } else if (value instanceof Long) {
                target.writeInt(LONG_TYPE_ID);
                target.writeLong((Long) value);
            } else if (value instanceof Boolean) {
                target.writeInt(BOOLEAN_TYPE_ID);
                target.writeBoolean((Boolean) value);
            } else {
                throw new IllegalArgumentException("Unknown value class " + value.getClass());
            }
        }
    }


    @Override
    protected ByteBuffer objectToBuffer(Object value, int estimatedSize) throws IOException, InterruptedException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(estimatedSize);
        DataOutputStream output = new DataOutputStream(byteStream);

        ADAPTER.writeObject(output, value);

        output.close();
        byteStream.close();
        byte[] bytes = byteStream.toByteArray();
        return new ByteBufferImpl(bytes, 0, bytes.length);
    }

    @Override
    public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException {
        try (DataInputStream inputSteam = new DataInputStream(new ByteArrayInputStream(buf))) {
            return ADAPTER.readObject(inputSteam);
        }
    }

    @Override
    public boolean isMarshallable(Object o) throws Exception {
        return supportedTypes.contains(o.getClass());
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_SERIALIZED_OBJECT;
    }

}
