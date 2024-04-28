/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2022 Vladimir Bukhtoyarov
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
package io.github.bucket4j.local;

import io.github.bucket4j.distributed.serialization.DataOutputSerializationAdapter;
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Versions;

import java.io.*;
import java.util.Map;

public class LocalBucketSerializationHelper {

    private static final DataOutputSerializationAdapter adapter = DataOutputSerializationAdapter.INSTANCE;

    static byte[] toBinarySnapshot(LocalBucket localBucket) throws IOException {
        SerializationHandle<LocalBucket> serializationHandle = getSerializationHandle(localBucket);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(baos);
        adapter.writeInt(output, serializationHandle.getTypeId());
        serializationHandle.serialize(adapter, output, localBucket, Versions.getLatest(), Scope.PERSISTED_STATE);
        return baos.toByteArray();
    }

    static LocalBucket fromBinarySnapshot(byte[] snapshot) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(snapshot);
        DataInputStream input = new DataInputStream(bais);
        int typeId = adapter.readInt(input);
        SerializationHandle<LocalBucket> serializationHandle = getSerializationHandle(typeId);
        return serializationHandle.deserialize(adapter, input);
    }

    static Map<String, Object> toJsonCompatibleSnapshot(LocalBucket bucket) throws IOException {
        SerializationHandle<LocalBucket> serializationHandle = getSerializationHandle(bucket);
        Map<String, Object> jsonMap = serializationHandle.toJsonCompatibleSnapshot(bucket, Versions.getLatest(), Scope.PERSISTED_STATE);
        jsonMap.put("type", serializationHandle.getTypeName());
        return jsonMap;
    }

    static LocalBucket fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
        String typeName = (String) snapshot.get("type");
        SerializationHandle<LocalBucket> serializationHandle = getSerializationHandle(typeName);
        return serializationHandle.fromJsonCompatibleSnapshot(snapshot);
    }

    private static SerializationHandle<LocalBucket> getSerializationHandle(LocalBucket localBucket) {
        return switch (localBucket.getSynchronizationStrategy()) {
            case LOCK_FREE -> (SerializationHandle) LockFreeBucket.SERIALIZATION_HANDLE;
            case SYNCHRONIZED -> (SerializationHandle) SynchronizedBucket.SERIALIZATION_HANDLE;
            case NONE -> (SerializationHandle) ThreadUnsafeBucket.SERIALIZATION_HANDLE;
        };
    }

    private static SerializationHandle<LocalBucket> getSerializationHandle(int typeId) throws IOException {
        if (typeId == LockFreeBucket.SERIALIZATION_HANDLE.getTypeId()) {
            return (SerializationHandle) LockFreeBucket.SERIALIZATION_HANDLE;
        } else if (typeId == SynchronizedBucket.SERIALIZATION_HANDLE.getTypeId()) {
            return (SerializationHandle) SynchronizedBucket.SERIALIZATION_HANDLE;
        } else if (typeId == ThreadUnsafeBucket.SERIALIZATION_HANDLE.getTypeId()) {
            return (SerializationHandle) ThreadUnsafeBucket.SERIALIZATION_HANDLE;
        } else {
            throw new IOException("Unknown typeId=" + typeId);
        }
    }

    private static SerializationHandle<LocalBucket> getSerializationHandle(String typeName) throws IOException {
        if (LockFreeBucket.SERIALIZATION_HANDLE.getTypeName().equals(typeName)) {
            return (SerializationHandle) LockFreeBucket.SERIALIZATION_HANDLE;
        } else if (SynchronizedBucket.SERIALIZATION_HANDLE.getTypeName().equals(typeName)) {
            return (SerializationHandle) SynchronizedBucket.SERIALIZATION_HANDLE;
        } else if (ThreadUnsafeBucket.SERIALIZATION_HANDLE.getTypeName().equals(typeName)) {
            return (SerializationHandle) ThreadUnsafeBucket.SERIALIZATION_HANDLE;
        } else {
            throw new IOException("Unknown typeName=" + typeName);
        }
    }

}
