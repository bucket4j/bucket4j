package io.github.bucket4j.serialization;

public interface SelfSerializable {

    <T> void serializeItself(SerializationBinding<T> binding, T target);

}
