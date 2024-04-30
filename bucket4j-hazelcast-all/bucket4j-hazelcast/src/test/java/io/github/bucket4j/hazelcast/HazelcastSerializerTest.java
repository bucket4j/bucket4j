package io.github.bucket4j.hazelcast;

import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.internal.nio.BufferObjectDataInput;
import com.hazelcast.internal.nio.BufferObjectDataOutput;
import com.hazelcast.internal.serialization.InternalSerializationService;
import com.hazelcast.internal.serialization.impl.DefaultSerializationServiceBuilder;
import com.hazelcast.nio.serialization.StreamSerializer;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.remote.commands.AddTokensCommand;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.grid.hazelcast.HazelcastEntryProcessor;
import io.github.bucket4j.grid.hazelcast.HazelcastOffloadableEntryProcessor;
import io.github.bucket4j.grid.hazelcast.SimpleBackupProcessor;
import io.github.bucket4j.grid.hazelcast.VersionedBackupProcessor;
import io.github.bucket4j.grid.hazelcast.serialization.HazelcastEntryProcessorSerializer;
import io.github.bucket4j.grid.hazelcast.serialization.HazelcastOffloadableEntryProcessorSerializer;
import io.github.bucket4j.grid.hazelcast.serialization.SimpleBackupProcessorSerializer;
import io.github.bucket4j.grid.hazelcast.serialization.VersionedBackupProcessorSerializer;
import io.github.bucket4j.util.ComparableByContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class HazelcastSerializerTest {

    private InternalSerializationService serializationService;
    private Map<Class<?>, StreamSerializer<?>> serializerByClass = new HashMap<>();

    @BeforeEach
    public void setup() {
        SerializationConfig serializationConfig = new SerializationConfig();

        HazelcastEntryProcessorSerializer processorSerializer = new HazelcastEntryProcessorSerializer(1000);
        serializationConfig.addSerializerConfig(
                new SerializerConfig()
                        .setImplementation(processorSerializer)
                        .setTypeClass(processorSerializer.getSerializableType())
        );

        SimpleBackupProcessorSerializer backupSerializer = new SimpleBackupProcessorSerializer(1001);
        serializationConfig.addSerializerConfig(
                new SerializerConfig()
                        .setImplementation(backupSerializer)
                        .setTypeClass(backupSerializer.getSerializableType())
        );

        HazelcastOffloadableEntryProcessorSerializer offloadableProcessorSerializer = new HazelcastOffloadableEntryProcessorSerializer(1002);
        serializationConfig.addSerializerConfig(
            new SerializerConfig()
                .setImplementation(offloadableProcessorSerializer)
                .setTypeClass(offloadableProcessorSerializer.getSerializableType())
        );

        VersionedBackupProcessorSerializer versionedBackupSerializer = new VersionedBackupProcessorSerializer(1003);
        serializationConfig.addSerializerConfig(
            new SerializerConfig()
                .setImplementation(versionedBackupSerializer)
                .setTypeClass(versionedBackupSerializer.getSerializableType())
        );

        serializerByClass.put(processorSerializer.getSerializableType(), processorSerializer);
        serializerByClass.put(backupSerializer.getSerializableType(), backupSerializer);
        serializerByClass.put(offloadableProcessorSerializer.getSerializableType(), offloadableProcessorSerializer);
        serializerByClass.put(versionedBackupSerializer.getSerializableType(), versionedBackupSerializer);

        this.serializationService = new DefaultSerializationServiceBuilder()
                .setConfig(serializationConfig)
                .build();
    }

    @Test
    public void tetsSerializationOfEntryProcessors() {
        RemoteCommand<?> command = new AddTokensCommand(42);
        Request request = new Request(command, Versions.getLatest(), null, null);
        testSerialization(new HazelcastEntryProcessor(request));
        testSerialization(new HazelcastOffloadableEntryProcessor<>(request, "my-custom-executor"));
        testSerialization(new SimpleBackupProcessor(new byte[] {1,2,3}));
        testSerialization(new VersionedBackupProcessor<>(new byte[] {1,2,3}, null));
        testSerialization(new VersionedBackupProcessor<>(new byte[] {1,2,3}, 42L));
    }

    private <T> T serializeAndDeserialize(T original) {
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

    private void testSerialization(Object object) {
        Object object2 = serializeAndDeserialize(object);
        assertTrue(ComparableByContent.equals(object, object2));
    }

}
