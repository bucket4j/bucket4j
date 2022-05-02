package io.github.bucket4j.distributed.serialization;

import io.github.bucket4j.distributed.versioning.Versions;

import java.util.HashMap;
import java.util.Map;

public class HashMapOutputSerializationTest extends AbstractSerializationTest {

    private Map<Class, SerializationHandle> allHandles = new HashMap<Class, SerializationHandle>()
    {{
        for (SerializationHandle<?> handle : SerializationHandles.CORE_HANDLES.getAllHandles()) {
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
            Map<String, Object> snapshot = serializationHandle.toJsonCompatibleSnapshot(object, Versions.getLatest());
            return (T) serializationHandle.fromJsonCompatibleSnapshot(snapshot, Versions.getLatest());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
