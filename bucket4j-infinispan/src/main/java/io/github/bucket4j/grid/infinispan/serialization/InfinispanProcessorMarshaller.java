package io.github.bucket4j.grid.infinispan.serialization;

import io.github.bucket4j.grid.infinispan.InfinispanProcessor;
import org.infinispan.protostream.MessageMarshaller;

import java.io.*;

public class InfinispanProcessorMarshaller implements MessageMarshaller<InfinispanProcessor> {

    private final String protoTypeName;

    public InfinispanProcessorMarshaller(String protoTypeName) {
        this.protoTypeName = protoTypeName;
    }

    @Override
    public InfinispanProcessor readFrom(ProtoStreamReader reader) throws IOException {
        byte bytes[] = reader.readBytes("data");
        return new InfinispanProcessor(bytes);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, InfinispanProcessor processor) throws IOException {
        writer.writeBytes("data", processor.getCommandBytes());
    }

    @Override
    public Class<InfinispanProcessor> getJavaClass() {
        return InfinispanProcessor.class;
    }

    @Override
    public String getTypeName() {
        return protoTypeName;
    }

}
