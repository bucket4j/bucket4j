package io.github.bucket4j.serialization;

import java.io.IOException;

public interface SerializationAdapter<T> {

    void writeBoolean(T target, boolean value) throws IOException;

    void writeByte(T target, byte value) throws IOException;

    void writeInt(T target, int value) throws IOException;

    void writeLong(T target, long value) throws IOException;

    void writeLongArray(T target, long[] value) throws IOException;

    void writeDoubleArray(T target, double[] value) throws IOException;

}