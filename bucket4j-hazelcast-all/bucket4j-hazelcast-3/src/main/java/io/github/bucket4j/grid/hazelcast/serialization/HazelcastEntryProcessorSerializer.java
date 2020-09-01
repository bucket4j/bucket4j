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
package io.github.bucket4j.grid.hazelcast.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.hazelcast.nio.serialization.TypedStreamDeserializer;
import io.github.bucket4j.grid.hazelcast.HazelcastEntryProcessor;

import java.io.IOException;


public class HazelcastEntryProcessorSerializer implements StreamSerializer<HazelcastEntryProcessor>, TypedStreamDeserializer<HazelcastEntryProcessor> {

    private final int typeId;

    public HazelcastEntryProcessorSerializer(int typeId) {
        this.typeId = typeId;
    }

    public Class<HazelcastEntryProcessor> getSerializableType() {
        return HazelcastEntryProcessor.class;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void write(ObjectDataOutput out, HazelcastEntryProcessor serializable) throws IOException {
        out.writeByteArray(serializable.getRequestBytes());
    }

    @Override
    public HazelcastEntryProcessor read(ObjectDataInput in) throws IOException {
        return read0(in);
    }

    @Override
    public HazelcastEntryProcessor read(ObjectDataInput in, Class aClass) throws IOException {
        return read0(in);
    }

    private HazelcastEntryProcessor read0(ObjectDataInput in) throws IOException {
        byte[] commandBytes = in.readByteArray();
        return new HazelcastEntryProcessor(commandBytes);
    }

}
