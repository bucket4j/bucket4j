package io.github.bucket4j.hazelcast;

import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.internal.serialization.InternalSerializationService;
import com.hazelcast.internal.serialization.impl.DefaultSerializationServiceBuilder;
import com.hazelcast.nio.BufferObjectDataInput;
import com.hazelcast.nio.BufferObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import io.github.bucket4j.grid.hazelcast.serialization.*;
import io.github.bucket4j.serialization.AbstractSerializationTest;
import org.junit.Before;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SerializerTest extends AbstractSerializationTest {

    private InternalSerializationService serializationService;
    private Map<Class<?>, HazelcastSerializer<?>> serializerByClass = new HashMap<>();


    @Before
    public void setup() {
        SerializationConfig serializationConfig = new SerializationConfig();
        for (HazelcastSerializer serializer : HazelcastSerializer.getAllSerializers(1000)) {
            serializationConfig.addSerializerConfig(
                new SerializerConfig()
                    .setImplementation(serializer)
                    .setTypeClass(serializer.getSerializableType())
            );

            serializerByClass.put(serializer.getSerializableType(), serializer);
        }

        this.serializationService = new DefaultSerializationServiceBuilder()
                .setConfig(serializationConfig)
                .build();
    }

    @Override
    protected <T> T serializeAndDeserialize(T original) {
        try {
            StreamSerializer<T> serializer = (StreamSerializer<T>) serializerByClass.get(original.getClass());

            BufferObjectDataOutput out = serializationService.createObjectDataOutput();
            serializer.write(out, original);

            BufferObjectDataInput in = serializationService.createObjectDataInput(out.toByteArray());
            return serializer.read(in);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
