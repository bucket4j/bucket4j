package io.github.bucket4j.grid.hazelcast.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;

import java.io.IOException;

public class HazelcastSerializationAdapter implements SerializationAdapter<ObjectDataOutput>, DeserializationAdapter<ObjectDataInput> {

    @Override
    public boolean readBoolean(ObjectDataInput source) throws IOException {
        return source.readBoolean();
    }

    @Override
    public byte readByte(ObjectDataInput source) throws IOException {
        return source.readByte();
    }

    @Override
    public int readInt(ObjectDataInput source) throws IOException {
        return source.readInt();
    }

    @Override
    public long readLong(ObjectDataInput source) throws IOException {
        return source.readLong();
    }

    @Override
    public long[] readLongArray(ObjectDataInput source) throws IOException {
        return source.readLongArray();
    }

    @Override
    public double[] readDoubleArray(ObjectDataInput source) throws IOException {
        return source.readDoubleArray();
    }

    @Override
    public void writeBoolean(ObjectDataOutput target, boolean value) throws IOException {
        target.writeBoolean(value);
    }

    @Override
    public void writeByte(ObjectDataOutput target, byte value) throws IOException {
        target.writeByte(value);
    }

    @Override
    public void writeInt(ObjectDataOutput target, int value) throws IOException {
        target.writeInt(value);
    }

    @Override
    public void writeLong(ObjectDataOutput target, long value) throws IOException {
        target.writeLong(value);
    }

    @Override
    public void writeLongArray(ObjectDataOutput target, long[] value) throws IOException {
        target.writeLongArray(value);
    }

    @Override
    public void writeDoubleArray(ObjectDataOutput target, double[] value) throws IOException {
        target.writeDoubleArray(value);
    }

}
