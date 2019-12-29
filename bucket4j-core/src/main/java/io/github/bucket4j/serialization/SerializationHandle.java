package io.github.bucket4j.serialization;


import java.io.IOException;

public interface SerializationHandle<T> {

    <I> T deserialize(DeserializationAdapter<I> adapter, I source) throws IOException;

    <O> void serialize(SerializationAdapter<O> adapter, O target, T serializableObject) throws IOException;

}
