package io.github.bucket4j.grid.infinispan.serialization;

import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.serialization.SerializationHandle;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.AbstractMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;

import java.io.*;


public class Bucket4jContextInitializer implements SerializationContextInitializer {

    @Override
    public String getProtoFileName() {
        return "bucket4j.proto";
    }

    @Override
    public String getProtoFile() throws UncheckedIOException {
        return FileDescriptorSource.getResourceAsString(getClass(), "/" + getProtoFileName());
    }

    @Override
    public void registerSchema(SerializationContext serCtx) {
        serCtx.registerProtoFiles(FileDescriptorSource.fromString(getProtoFileName(), getProtoFile()));
    }

    @Override
    public void registerMarshallers(SerializationContext serCtx) {
        for (SerializationHandle<?> serializationHandle : Bucket4j.getSerializationHandles()) {
            serCtx.registerMarshaller(new ProtobufMessageMarshaller<>(serializationHandle));
        }
    }

}
