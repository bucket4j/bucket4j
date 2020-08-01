
package io.github.bucket4j.distributed.serialization;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class DataOutputSerializationAdapter implements SerializationAdapter<DataOutput>, DeserializationAdapter<DataInput> {

    public static DataOutputSerializationAdapter INSTANCE = new DataOutputSerializationAdapter();

    private DataOutputSerializationAdapter() {}

    @Override
    public boolean readBoolean(DataInput source) throws IOException {
        return source.readBoolean();
    }

    @Override
    public byte readByte(DataInput source) throws IOException {
        return source.readByte();
    }

    @Override
    public int readInt(DataInput source) throws IOException {
        return source.readInt();
    }

    @Override
    public long readLong(DataInput source) throws IOException {
        return source.readLong();
    }

    @Override
    public long[] readLongArray(DataInput source) throws IOException {
        int size = source.readInt();
        long array[] = new long[size];
        for (int i = 0; i < size; i++) {
            array[i] = source.readLong();
        }
        return array;
    }

    @Override
    public double[] readDoubleArray(DataInput source) throws IOException {
        int size = source.readInt();
        double array[] = new double[size];
        for (int i = 0; i < size; i++) {
            array[i] = source.readDouble();
        }
        return array;
    }

    @Override
    public void writeBoolean(DataOutput target, boolean value) throws IOException {
        target.writeBoolean(value);
    }

    @Override
    public void writeByte(DataOutput target, byte value) throws IOException {
        target.writeByte(value);
    }

    @Override
    public void writeInt(DataOutput target, int value) throws IOException {
        target.writeInt(value);
    }

    @Override
    public void writeLong(DataOutput target, long value) throws IOException {
        target.writeLong(value);
    }

    @Override
    public void writeLongArray(DataOutput target, long[] value) throws IOException {
        target.writeInt(value.length);
        for (int i = 0; i < value.length; i++) {
            target.writeLong(value[i]);
        }
    }

    @Override
    public void writeDoubleArray(DataOutput target, double[] value) throws IOException {
        target.writeInt(value.length);
        for (int i = 0; i < value.length; i++) {
            target.writeDouble(value[i]);
        }
    }

}
