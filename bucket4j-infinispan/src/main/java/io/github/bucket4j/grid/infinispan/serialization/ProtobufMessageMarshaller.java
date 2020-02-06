package io.github.bucket4j.grid.infinispan.serialization;

import io.github.bucket4j.serialization.DataStreamAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import org.infinispan.protostream.MessageMarshaller;

import java.io.*;

public class ProtobufMessageMarshaller<T> implements MessageMarshaller<T> {

    private static DataStreamAdapter ADAPTER = new DataStreamAdapter();

    private final SerializationHandle<T> serializationHandle;
    private final String protoTypeName;

    public ProtobufMessageMarshaller(SerializationHandle<T> serializationHandle, String protoTypeName) {
        this.serializationHandle = serializationHandle;
        this.protoTypeName = protoTypeName;
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
        return protoTypeName;
    }

}
