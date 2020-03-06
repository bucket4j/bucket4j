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
