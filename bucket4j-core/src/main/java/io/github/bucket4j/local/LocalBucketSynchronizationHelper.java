package io.github.bucket4j.local;

import io.github.bucket4j.distributed.serialization.DataOutputSerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Versions;

import java.io.*;
import java.util.Map;

public class LocalBucketSynchronizationHelper {

    private static final DataOutputSerializationAdapter adapter = DataOutputSerializationAdapter.INSTANCE;

    static byte[] toBinarySnapshot(LocalBucket localBucket) throws IOException {
        SerializationHandle<LocalBucket> serializationHandle = getSerializationHandle(localBucket);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(baos);
        adapter.writeInt(output, serializationHandle.getTypeId());
        serializationHandle.serialize(adapter, output, localBucket, Versions.getLatest());
        return baos.toByteArray();
    }

    static LocalBucket fromBinarySnapshot(byte[] snapshot) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(snapshot);
        DataInputStream input = new DataInputStream(bais);
        int typeId = adapter.readInt(input);
        SerializationHandle<LocalBucket> serializationHandle = getSerializationHandle(typeId);
        return serializationHandle.deserialize(adapter, input, Versions.getLatest());
    }

    static Map<String, Object> toJsonCompatibleSnapshot(LocalBucket bucket) throws IOException {
        SerializationHandle<LocalBucket> serializationHandle = getSerializationHandle(bucket);
        Map<String, Object> jsonMap = serializationHandle.toJsonCompatibleSnapshot(bucket, Versions.getLatest());
        jsonMap.put("type", serializationHandle.getTypeName());
        return jsonMap;
    }

    static LocalBucket fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
        String typeName = (String) snapshot.get("type");
        SerializationHandle<LocalBucket> serializationHandle = getSerializationHandle(typeName);
        return serializationHandle.fromJsonCompatibleSnapshot(snapshot, Versions.getLatest());
    }

    private static SerializationHandle<LocalBucket> getSerializationHandle(LocalBucket localBucket) throws IOException {
        switch (localBucket.getSynchronizationStrategy()) {
            case LOCK_FREE: return (SerializationHandle) LockFreeBucket.SERIALIZATION_HANDLE;
            case SYNCHRONIZED: return (SerializationHandle) SynchronizedBucket.SERIALIZATION_HANDLE;
            case NONE: return (SerializationHandle) ThreadUnsafeBucket.SERIALIZATION_HANDLE;
            default: throw new IOException("Unknown SynchronizationStrategy:" + localBucket.getSynchronizationStrategy());
        }
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