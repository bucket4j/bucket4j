package io.github.bucket4j.serialization;

import java.io.IOException;

public interface SelfSerializable {

    <T> void serializeItself(SerializationBinding<T> binding, T target) throws IOException;

}
