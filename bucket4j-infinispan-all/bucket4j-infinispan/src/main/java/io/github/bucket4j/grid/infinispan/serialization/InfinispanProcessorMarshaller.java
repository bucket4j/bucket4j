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

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

import io.github.bucket4j.grid.infinispan.InfinispanProcessor;

public class InfinispanProcessorMarshaller implements MessageMarshaller<InfinispanProcessor> {

    private final String protoTypeName;

    public InfinispanProcessorMarshaller(String protoTypeName) {
        this.protoTypeName = protoTypeName;
    }

    @Override
    public InfinispanProcessor readFrom(ProtoStreamReader reader) throws IOException {
        byte[] bytes = reader.readBytes("data");
        return new InfinispanProcessor(bytes);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, InfinispanProcessor processor) throws IOException {
        writer.writeBytes("data", processor.getRequestBytes());
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
