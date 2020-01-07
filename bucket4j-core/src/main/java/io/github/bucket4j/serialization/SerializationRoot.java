package io.github.bucket4j.serialization;

public interface SerializationRoot<T> {

    <T> SerializationHandle<T> getSerializationHandle();

}
