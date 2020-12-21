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
/*
 *
 * Copyright 2015-2020 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.serialization;

import io.github.bucket4j.Bucket4j;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DataStreamAdapter implements SerializationAdapter<DataOutput>, DeserializationAdapter<DataInput> {

    private static final Map<Integer, SerializationHandle<?>> serializerById = new HashMap<>();
    private static final Map<Class<?>, SerializationHandle<?>> serializerByClass = new HashMap<>();
    private static final Set<Class<?>> supportedTypes = new HashSet<>();

    private static final int LONG_TYPE_ID = -1;
    private static final int BOOLEAN_TYPE_ID = -2;
    private static final int NULL_TYPE_ID = -3;

    static {
        for (SerializationHandle<?> serializer : Bucket4j.getSerializationHandles()) {
            serializerById.put(serializer.getTypeId(), serializer);
            serializerByClass.put(serializer.getSerializedType(), serializer);
            supportedTypes.add(serializer.getSerializedType());
        }
    }

    @Override
    public boolean readBoolean(DataInput source) throws IOException {
        return source.readBoolean();
    }

    @Override
    public byte readByte(DataInput source) throws IOException {
        return source.readByte();
    }

    @Override
    public int readInt(DataInput source) throws IOException {
        return source.readInt();
    }

    @Override
    public long readLong(DataInput source) throws IOException {
        return source.readLong();
    }

    @Override
    public long[] readLongArray(DataInput source) throws IOException {
        int size = source.readInt();
        long array[] = new long[size];
        for (int i = 0; i < size; i++) {
            array[i] = source.readLong();
        }
        return array;
    }

    @Override
    public String readString(DataInput source) throws IOException {
        return source.readUTF();
    }

    @Override
    public void writeString(DataOutput target, String value) throws IOException {
        target.writeUTF(value);
    }

    @Override
    public <T> T readObject(DataInput source, Class<T> type) throws IOException {
        return (T) readObject(source);
    }

    @Override
    public Object readObject(DataInput source) throws IOException {
        int typeId = source.readInt();
        if (typeId > 0) {
            SerializationHandle serializer = serializerById.get(typeId);
            if (serializer == null) {
                throw new IllegalStateException("Unknown type id " + typeId);
            }
            return serializer.deserialize(this, source);
        } else {
            switch (typeId) {
                case BOOLEAN_TYPE_ID: return source.readBoolean();
                case LONG_TYPE_ID: return source.readLong();
                case NULL_TYPE_ID: return null;
                default: throw new IllegalStateException("Unknown type id " + typeId);
            }
        }
    }

    @Override
    public void writeBoolean(DataOutput target, boolean value) throws IOException {
        target.writeBoolean(value);
    }

    @Override
    public void writeByte(DataOutput target, byte value) throws IOException {
        target.writeByte(value);
    }

    @Override
    public void writeInt(DataOutput target, int value) throws IOException {
        target.writeInt(value);
    }

    @Override
    public void writeLong(DataOutput target, long value) throws IOException {
        target.writeLong(value);
    }

    @Override
    public void writeLongArray(DataOutput target, long[] value) throws IOException {
        target.writeInt(value.length);
        for (int i = 0; i < value.length; i++) {
            target.writeLong(value[i]);
        }
    }

    @Override
    public void writeObject(DataOutput target, Object value) throws IOException {
        if (value == null) {
            target.writeInt(NULL_TYPE_ID);
            return;
        }
        SerializationHandle serializer = serializerByClass.get(value.getClass());
        if (serializer != null) {
            target.writeInt(serializer.getTypeId());
            serializer.serialize(this, target, value);
        } else if (value instanceof Long) {
            target.writeInt(LONG_TYPE_ID);
            target.writeLong((Long) value);
        } else if (value instanceof Boolean) {
            target.writeInt(BOOLEAN_TYPE_ID);
            target.writeBoolean((Boolean) value);
        } else {
            throw new IllegalArgumentException("Unknown value class " + value.getClass());
        }
    }

}
