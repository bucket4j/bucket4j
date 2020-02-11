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


public class Bucket4jProtobufContextInitializer implements SerializationContextInitializer {

    private static final String FOOTER = "package bucket4j;\n";

    private static final String TYPE_TEMPLATE = "message [type_name] {\n" +
            "\n" +
            "    required bytes data = 1;\n" +
            "\n" +
            "}\n\n";

    @Override
    public String getProtoFileName() {
        return "bucket4j.proto";
    }

    @Override
    public String getProtoFile() throws UncheckedIOException {
        return "/" + getProtoFileName();
    }

    @Override
    public void registerSchema(SerializationContext serCtx) {
        StringBuilder protoBuilder = new StringBuilder(FOOTER);

        for (SerializationHandle<?> serializationHandle : Bucket4j.getSerializationHandles()) {
            String typeName = "Bucket4jType_" + serializationHandle.getTypeId();
            String typeDefinition = TYPE_TEMPLATE.replace("[type_name]", typeName);
            protoBuilder.append(typeDefinition);
        }

        String generatedProtoFile = protoBuilder.toString();
        FileDescriptorSource protoSource = FileDescriptorSource.fromString(getProtoFileName(), generatedProtoFile);
        serCtx.registerProtoFiles(protoSource);
    }

    @Override
    public void registerMarshallers(SerializationContext serCtx) {
        for (SerializationHandle<?> serializationHandle : Bucket4j.getSerializationHandles()) {
            String protoTypeId = "bucket4j.Bucket4jType_" + serializationHandle.getTypeId();
            serCtx.registerMarshaller(new ProtobufMessageMarshaller<>(serializationHandle, protoTypeId));
        }
    }

}
