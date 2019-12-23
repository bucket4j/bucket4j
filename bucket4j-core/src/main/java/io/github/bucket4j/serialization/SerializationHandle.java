package io.github.bucket4j.serialization;


import java.io.IOException;

public interface SerializationHandle<T> {

    <I> T deserialize(DeserializationBinding<I> binding, I source) throws IOException;

    <O> void serialize(SerializationBinding<O> binding, O target, T serializableObject) throws IOException;

}
