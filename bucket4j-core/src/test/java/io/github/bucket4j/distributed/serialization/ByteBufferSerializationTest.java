package io.github.bucket4j.distributed.serialization;

import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that {@link ByteBufferSerializationAdapter} - combined with {@link SerializationHandle#estimateSize} -
 * produces byte-for-byte identical output to {@link DataOutputSerializationAdapter}, and that bytes written by
 * either adapter can be read back by the other one.
 */
public class ByteBufferSerializationTest extends AbstractSerializationTest {

    private Map<Class, SerializationHandle> allHandles = new HashMap<>()
    {{
        for (SerializationHandle<?> handle : SerializationHandles.CORE_HANDLES.getAllHandles()) {
            put(handle.getSerializedType(), handle);
        }
    }};

    @Override
    protected <T> T serializeAndDeserialize(T object, Scope scope) {
        SerializationHandle serializationHandle = allHandles.get(object.getClass());
        if (serializationHandle == null) {
            throw new IllegalArgumentException("Serializer for class " + serializationHandle + " is not specified");
        }
        try {
            int estimatedSize = serializationHandle.estimateSize(object, Versions.getLatest(), scope);
            ByteBuffer buffer = ByteBuffer.allocate(estimatedSize);
            serializationHandle.serialize(ByteBufferSerializationAdapter.INSTANCE, buffer, object, Versions.getLatest(), scope);
            assertEquals(estimatedSize, buffer.position(), "estimateSize() did not match the actual number of bytes written for " + object.getClass());
            byte[] byteBufferBytes = buffer.array();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            serializationHandle.serialize(DataOutputSerializationAdapter.INSTANCE, dos, object, Versions.getLatest(), scope);
            byte[] dataOutputBytes = baos.toByteArray();

            assertArrayEquals(dataOutputBytes, byteBufferBytes, "ByteBufferSerializationAdapter and DataOutputSerializationAdapter produced different bytes for " + object.getClass());

            // bytes written by the ByteBuffer adapter must be readable by the DataOutput adapter
            T deserializedByDataInput = (T) serializationHandle.deserialize(DataOutputSerializationAdapter.INSTANCE, new DataInputStream(new ByteArrayInputStream(byteBufferBytes)));
            if (!ComparableByContent.equals(object, deserializedByDataInput)) {
                throw new IllegalStateException("Bytes written by ByteBufferSerializationAdapter were not correctly read back by DataOutputSerializationAdapter for " + object.getClass());
            }

            // bytes written by the DataOutput adapter must be readable by the ByteBuffer adapter
            return (T) serializationHandle.deserialize(ByteBufferSerializationAdapter.INSTANCE, ByteBuffer.wrap(dataOutputBytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
