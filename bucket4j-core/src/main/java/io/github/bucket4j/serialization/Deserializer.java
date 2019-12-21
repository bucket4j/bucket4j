package io.github.bucket4j.serialization;


import java.io.IOException;

public interface Deserializer<T extends SelfSerializable> {

    <S> T deserialize(DeserializationBinding<S> binding, S source) throws IOException;

}
