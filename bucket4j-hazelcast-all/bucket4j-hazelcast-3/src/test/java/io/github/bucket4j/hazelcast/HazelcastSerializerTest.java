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
import io.github.bucket4j.EqualityUtils;
import io.github.bucket4j.grid.AddTokensCommand;
import io.github.bucket4j.grid.GridCommand;
import io.github.bucket4j.grid.hazelcast.serialization.*;
import io.github.bucket4j.grid.jcache.ExecuteProcessor;
import io.github.bucket4j.grid.jcache.InitStateAndExecuteProcessor;
import io.github.bucket4j.grid.jcache.InitStateProcessor;
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

    static {
        EqualityUtils.registerComparator(ExecuteProcessor.class, (processor1, processor2) -> {
            return EqualityUtils.equals(processor1.getTargetCommand(), processor2.getTargetCommand());
        });

        EqualityUtils.registerComparator(InitStateProcessor.class, (processor1, processor2) -> {
            return EqualityUtils.equals(processor1.getConfiguration(), processor2.getConfiguration());
        });

        EqualityUtils.registerComparator(InitStateAndExecuteProcessor.class, (processor1, processor2) -> {
            return EqualityUtils.equals(processor1.getTargetCommand(), processor2.getTargetCommand())
                    && EqualityUtils.equals(processor1.getConfiguration(), processor2.getConfiguration());
        });
    }

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
        GridCommand command = new AddTokensCommand(42);

        testSerialization(new InitStateProcessor<>(configuration));
        testSerialization(new ExecuteProcessor<>(command));
        testSerialization(new InitStateAndExecuteProcessor(command, configuration));
    }

}
