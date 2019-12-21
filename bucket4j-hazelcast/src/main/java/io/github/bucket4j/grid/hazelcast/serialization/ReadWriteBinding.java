package io.github.bucket4j.grid.hazelcast.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import io.github.bucket4j.serialization.DeserializationBinding;
import io.github.bucket4j.serialization.SerializationBinding;

import java.io.IOException;

class ReadWriteBinding implements SerializationBinding<ObjectDataOutput>, DeserializationBinding<ObjectDataInput> {

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
    public <T> T readObject(ObjectDataInput source, Class<T> type) throws IOException {
        return source.readObject(type);
    }

    @Override
    public Object readObject(ObjectDataInput source) throws IOException {
        return source.readObject();
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
    public void writeObject(ObjectDataOutput target, Object value) throws IOException {
        target.writeObject(value);
    }

}
