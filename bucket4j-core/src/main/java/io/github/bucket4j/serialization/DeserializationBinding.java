package io.github.bucket4j.serialization;

public interface DeserializationBinding<S> {

    boolean readBoolean(S source);

    byte readByte(S source);

    int readInt(S source);

    long readLong(S source);

    long[] readLongArray(S source);

    <T> T readObject(S source);

}