package io.github.bucket4j.hazelcast.serialization;

import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.internal.serialization.InternalSerializationService;
import com.hazelcast.internal.serialization.impl.DefaultSerializationServiceBuilder;
import com.hazelcast.nio.BufferObjectDataInput;
import com.hazelcast.nio.BufferObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import io.github.bucket4j.grid.hazelcast.serialization.*;
import org.junit.Before;

import java.io.IOException;

public abstract class SerializerTest<T> {

    private InternalSerializationService serializationService;

    @Before
    public void setup() {
        SerializationConfig serializationConfig = new SerializationConfig();
        for (HazelcastSerializer serializer : HazelcastSerializer.getAllSerializers(1000)) {
            serializationConfig.addSerializerConfig(
                new SerializerConfig()
                    .setImplementation(serializer)
                    .setTypeClass(serializer.getSerializableType())
            );
        }

        this.serializationService = new DefaultSerializationServiceBuilder()
                .setConfig(serializationConfig)
                .build();
    }

    protected abstract StreamSerializer<T> getSerializerUnderTest();

    protected abstract void runAssertions(T original, T deserialized);

    void testSerialization(T original) throws IOException {
        StreamSerializer<T> serializer = getSerializerUnderTest();

        BufferObjectDataOutput out = serializationService.createObjectDataOutput();
        serializer.write(out, original);

        BufferObjectDataInput in = serializationService.createObjectDataInput(out.toByteArray());
        T deserialized = serializer.read(in);

        runAssertions(original, deserialized);
    }
}
