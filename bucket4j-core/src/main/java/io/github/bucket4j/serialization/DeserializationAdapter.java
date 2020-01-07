package io.github.bucket4j.serialization;

import java.io.IOException;

public interface DeserializationAdapter<S> {

    boolean readBoolean(S source) throws IOException;

    byte readByte(S source) throws IOException;

    int readInt(S source) throws IOException;

    long readLong(S source) throws IOException;

    long[] readLongArray(S source) throws IOException;

    double[] readDoubleArray(S source) throws IOException;

}