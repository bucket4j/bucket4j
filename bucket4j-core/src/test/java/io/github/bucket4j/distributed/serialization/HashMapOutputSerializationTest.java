package io.github.bucket4j.distributed.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.bucket4j.distributed.versioning.Versions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class HashMapOutputSerializationTest extends AbstractSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(HashMapOutputSerializationTest.class);

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
            Map<String, Object> snapshot = serializationHandle.toJsonCompatibleSnapshot(object, Versions.getLatest(), scope);
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);;
            String snapshotString = mapper.writeValueAsString(snapshot);
            logger.info("----------------------------------------------------------");
            logger.info("{}:\n{}", object.getClass().getName(), snapshotString);
            logger.info("----------------------------------------------------------");

            Map<String, Object> deserializedSnapshot = mapper.readValue(snapshotString, Map.class);
            return (T) serializationHandle.fromJsonCompatibleSnapshot(deserializedSnapshot);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
