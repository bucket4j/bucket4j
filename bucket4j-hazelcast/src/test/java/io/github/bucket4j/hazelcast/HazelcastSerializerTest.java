package io.github.bucket4j.hazelcast;

import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.internal.serialization.InternalSerializationService;
import com.hazelcast.internal.serialization.impl.DefaultSerializationServiceBuilder;
import com.hazelcast.nio.BufferObjectDataInput;
import com.hazelcast.nio.BufferObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.MathType;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.AddTokensCommand;
import io.github.bucket4j.grid.hazelcast.SimpleBackupProcessor;
import io.github.bucket4j.grid.hazelcast.serialization.*;
import io.github.bucket4j.serialization.AbstractSerializationTest;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.github.bucket4j.Bandwidth.simple;
import static java.time.Duration.ofSeconds;

public class HazelcastSerializerTest extends AbstractSerializationTest {

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

    @Test
    public void tetsSerializationOfEntryProcessors() {
        BucketConfiguration configuration = Bucket4j.configurationBuilder()
                .addLimit(simple(10, ofSeconds(1)))
                .build();
        RemoteCommand<?> command = new AddTokensCommand(42);
        BucketState bucketState = BucketState.createInitialState(configuration, MathType.INTEGER_64_BITS, System.nanoTime());
        RemoteBucketState gridBucketState = new RemoteBucketState(configuration, bucketState);

        //testSerialization(new HazelcastEntryProcessor<>(command));
        testSerialization(new SimpleBackupProcessor<>(gridBucketState));
    }

}
