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

import java.io.IOException;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.hazelcast.nio.serialization.TypedStreamDeserializer;

import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.grid.hazelcast.VersionedBackupProcessor;

import static io.github.bucket4j.distributed.versioning.Versions.v_8_10_0;


public class VersionedBackupProcessorSerializer implements StreamSerializer<VersionedBackupProcessor>, TypedStreamDeserializer<VersionedBackupProcessor> {

    private final int typeId;

    public VersionedBackupProcessorSerializer(int typeId) {
        this.typeId = typeId;
    }
    public VersionedBackupProcessorSerializer() {
        this.typeId = SerializationUtilities.getSerializerTypeId(this.getClass());
    }

    public Class<VersionedBackupProcessor> getSerializableType() {
        return VersionedBackupProcessor.class;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void write(ObjectDataOutput out, VersionedBackupProcessor serializable) throws IOException {
        out.writeInt(v_8_10_0.getNumber());
        out.writeByteArray(serializable.getState());
        if (serializable.getTtlMillis() != null) {
            out.writeBoolean(true);
            out.writeLong(serializable.getTtlMillis());
        } else {
            out.writeBoolean(false);
        }
    }

    @Override
    public VersionedBackupProcessor read(ObjectDataInput in) throws IOException {
        return read0(in);
    }

    @Override
    public VersionedBackupProcessor read(ObjectDataInput in, Class aClass) throws IOException {
        return read0(in);
    }

    private VersionedBackupProcessor read0(ObjectDataInput in) throws IOException {
        int version = in.readInt();
        Versions.check(version, v_8_10_0, v_8_10_0);
        byte[] state = in.readByteArray();
        Long ttlMillis = in.readBoolean() ? in.readLong() : null;
        return new VersionedBackupProcessor(state, ttlMillis);
    }

}
