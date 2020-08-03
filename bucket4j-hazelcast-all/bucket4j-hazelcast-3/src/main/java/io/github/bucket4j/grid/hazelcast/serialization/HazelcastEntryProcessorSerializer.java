package io.github.bucket4j.grid.hazelcast.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.hazelcast.nio.serialization.TypedStreamDeserializer;
import io.github.bucket4j.grid.hazelcast.HazelcastEntryProcessor;

import java.io.IOException;


public class HazelcastEntryProcessorSerializer implements StreamSerializer<HazelcastEntryProcessor>, TypedStreamDeserializer<HazelcastEntryProcessor> {

    private final int typeId;

    public HazelcastEntryProcessorSerializer(int typeId) {
        this.typeId = typeId;
    }

    public Class<HazelcastEntryProcessor> getSerializableType() {
        return HazelcastEntryProcessor.class;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void write(ObjectDataOutput out, HazelcastEntryProcessor serializable) throws IOException {
        out.writeByteArray(serializable.getRequestBytes());
    }

    @Override
    public HazelcastEntryProcessor read(ObjectDataInput in) throws IOException {
        return read0(in);
    }

    @Override
    public HazelcastEntryProcessor read(ObjectDataInput in, Class aClass) throws IOException {
        return read0(in);
    }

    private HazelcastEntryProcessor read0(ObjectDataInput in) throws IOException {
        byte[] commandBytes = in.readByteArray();
        return new HazelcastEntryProcessor(commandBytes);
    }

}
