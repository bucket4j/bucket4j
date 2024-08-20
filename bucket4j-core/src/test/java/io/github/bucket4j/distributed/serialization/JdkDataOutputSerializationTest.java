package io.github.bucket4j.distributed.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map;

import io.github.bucket4j.distributed.versioning.Versions;

public class JdkDataOutputSerializationTest extends AbstractSerializationTest {

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
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            serializationHandle.serialize(DataOutputSerializationAdapter.INSTANCE, dos, object, Versions.getLatest(), scope);
            byte[] bytes = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            DataInputStream input = new DataInputStream(bais);
            T deserialized = (T) serializationHandle.deserialize(DataOutputSerializationAdapter.INSTANCE, input);
            if (input.available() > 0) {
                throw new IllegalStateException("Input stream was npt read to the end fo class " + object.getClass());
            }
            return deserialized;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
