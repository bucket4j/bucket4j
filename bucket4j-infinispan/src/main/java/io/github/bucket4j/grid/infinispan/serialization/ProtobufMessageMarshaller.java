package io.github.bucket4j.grid.infinispan.serialization;

import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.serialization.SerializationHandle;
import org.infinispan.protostream.MessageMarshaller;

import java.io.*;

public class ProtobufMessageMarshaller<T> implements MessageMarshaller<T> {

    private static InfinispanSerializationAdapter ADAPTER = new InfinispanSerializationAdapter();

    private final SerializationHandle<T> serializationHandle;

    public ProtobufMessageMarshaller(SerializationHandle<T> serializationHandle) {
        this.serializationHandle = serializationHandle;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        byte bytes[] = reader.readBytes("data");

        try (DataInputStream inputSteam = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return (T) ADAPTER.readObject(inputSteam);
        }
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T value) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(byteStream);

        ADAPTER.writeObject(output, value);

        output.close();
        byteStream.close();
        byte[] bytes = byteStream.toByteArray();
        writer.writeBytes("data", bytes);
    }

    @Override
    public Class<T> getJavaClass() {
        return serializationHandle.getSerializedType();
    }

    @Override
    public String getTypeName() {
        return "bucket4j.RawData";
    }

}
