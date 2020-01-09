package io.github.bucket4j.serialization;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class JdkDataOutputSerializationTest extends AbstractSerializationTest {

    private Map<Class, SerializationHandle> allHandles = new HashMap<Class, SerializationHandle>()
    {{
        for (SerializationHandle<?> handle : SerializationHandle.CORE_HANDLES.getAllHandles()) {
            put(handle.getSerializedType(), handle);
        }
    }};

    @Override
    protected <T> T serializeAndDeserialize(T object) {
        SerializationHandle serializationHandle = allHandles.get(object.getClass());
        if (serializationHandle == null) {
            throw new IllegalArgumentException("Serializer for class " + serializationHandle + " is not specified");
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            serializationHandle.serialize(DataOutputSerializationAdapter.INSTANCE, dos, object);
            byte[] bytes = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            DataInputStream dis = new DataInputStream(bais);
            return (T) serializationHandle.deserialize(DataOutputSerializationAdapter.INSTANCE, dis);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
