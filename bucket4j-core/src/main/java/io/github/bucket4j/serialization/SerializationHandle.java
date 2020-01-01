package io.github.bucket4j.serialization;


import java.io.IOException;

public interface SerializationHandle<T> {

    <I> T deserialize(DeserializationAdapter<I> adapter, I input) throws IOException;

    <O> void serialize(SerializationAdapter<O> adapter, O output, T serializableObject) throws IOException;

    /**
     * @return the type identifier that is unique across all Bucket4j classes
     */
    int getTypeId();

    Class<T> getSerializedType();

}
