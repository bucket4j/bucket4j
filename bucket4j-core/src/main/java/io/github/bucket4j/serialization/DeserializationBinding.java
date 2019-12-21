package io.github.bucket4j.serialization;

import java.io.IOException;

public interface DeserializationBinding<S> {

    boolean readBoolean(S source) throws IOException;

    byte readByte(S source) throws IOException;

    int readInt(S source) throws IOException;

    long readLong(S source) throws IOException;

    long[] readLongArray(S source) throws IOException;

    <T> T readObject(S source, Class<T> type) throws IOException;

    Object readObject(S source) throws IOException;

}