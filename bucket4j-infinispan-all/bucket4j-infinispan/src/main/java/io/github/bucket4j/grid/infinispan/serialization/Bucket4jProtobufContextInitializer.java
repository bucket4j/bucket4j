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

import java.io.UncheckedIOException;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;


public class Bucket4jProtobufContextInitializer implements SerializationContextInitializer {

    private static final String FOOTER = "package bucket4j;\n";

    private static final String TYPE_TEMPLATE = """
        message [type_name] {

            required bytes data = 1;

        }
        """;

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
