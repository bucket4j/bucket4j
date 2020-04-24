package io.github.bucket4j.grid.infinispan.serialization;

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

        protoBuilder.append(TYPE_TEMPLATE.replace("[type_name]", "InfinispanProcessor"));

        String generatedProtoFile = protoBuilder.toString();
        FileDescriptorSource protoSource = FileDescriptorSource.fromString(getProtoFileName(), generatedProtoFile);
        serCtx.registerProtoFiles(protoSource);
    }

    @Override
    public void registerMarshallers(SerializationContext serCtx) {
        serCtx.registerMarshaller(new InfinispanProcessorMarshaller("bucket4j.InfinispanProcessor"));
    }

}
