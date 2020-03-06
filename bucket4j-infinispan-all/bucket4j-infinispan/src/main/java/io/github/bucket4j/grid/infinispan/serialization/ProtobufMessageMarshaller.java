/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
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
