package io.github.bucket4j.serialization;


public interface Deserializer<T extends SelfSerializable, S> {

    T deserialize(DeserializationBinding<S> binding, S source);

}
