package io.github.bucket4j.grid.hazelcast.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.hazelcast.nio.serialization.TypedStreamDeserializer;
import io.github.bucket4j.grid.hazelcast.SimpleBackupProcessor;

import java.io.IOException;


public class SimpleBackupProcessorSerializer implements StreamSerializer<SimpleBackupProcessor>, TypedStreamDeserializer<SimpleBackupProcessor> {

    private final int typeId;

    public SimpleBackupProcessorSerializer(int typeId) {
        this.typeId = typeId;
    }

    public Class<SimpleBackupProcessor> getSerializableType() {
        return SimpleBackupProcessor.class;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void write(ObjectDataOutput out, SimpleBackupProcessor serializable) throws IOException {
        out.writeByteArray(serializable.getState());
    }

    @Override
    public SimpleBackupProcessor read(ObjectDataInput in) throws IOException {
        return read0(in);
    }

    @Override
    public SimpleBackupProcessor read(ObjectDataInput in, Class aClass) throws IOException {
        return read0(in);
    }

    private SimpleBackupProcessor read0(ObjectDataInput in) throws IOException {
        return new SimpleBackupProcessor(in.readByteArray());
    }

}
