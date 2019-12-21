package io.github.bucket4j.serialization;

public interface SerializationBinding<T> {

    void writeBoolean(T target, boolean value);

    void writeByte(T target, byte value);

    void writeInt(T target, int value);

    void writeLong(T target, long value);

    void writeLongArray(T target, long[] value);

    void writeObject(T target, Object value);

}