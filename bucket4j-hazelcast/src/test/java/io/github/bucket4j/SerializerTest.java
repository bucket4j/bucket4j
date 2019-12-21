package io.github.bucket4j;

import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.internal.serialization.InternalSerializationService;
import com.hazelcast.internal.serialization.impl.DefaultSerializationServiceBuilder;
import com.hazelcast.nio.BufferObjectDataInput;
import com.hazelcast.nio.BufferObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import org.junit.Before;

import java.io.IOException;

public abstract class SerializerTest<T> {

    BandwidthSerializer bandwidthSerializer = new BandwidthSerializer(1);
    BucketConfigurationSerializer bucketConfigurationSerializer = new BucketConfigurationSerializer(2);
    BucketStateSerializer bucketStateSerializer = new BucketStateSerializer(3);
    CommandResultSerializer commandResultSerializer = new CommandResultSerializer(4);
    GridBucketStateSerializer gridBucketStateSerializer = new GridBucketStateSerializer(5);

    private InternalSerializationService serializationService;

    @Before
    public void setup() {
        SerializationConfig serializationConfig = new SerializationConfig()
                .addSerializerConfig(new SerializerConfig()
                        .setImplementation(bandwidthSerializer)
                        .setTypeClass(Bandwidth.class))
                .addSerializerConfig(new SerializerConfig()
                        .setImplementation(bucketConfigurationSerializer)
                        .setTypeClass(BucketConfiguration.class))
                .addSerializerConfig(new SerializerConfig()
                        .setImplementation(bucketStateSerializer)
                        .setTypeClass(BucketState.class))
                .addSerializerConfig(new SerializerConfig()
                        .setImplementation(commandResultSerializer)
                        .setTypeClass(CommandResult.class))
                .addSerializerConfig(new SerializerConfig()
                        .setImplementation(gridBucketStateSerializer)
                        .setTypeClass(GridBucketState.class));

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
