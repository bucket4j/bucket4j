package io.github.bucket4j.distributed.serialization;

import io.github.bucket4j.distributed.versioning.UnsupportedTypeException;
import io.github.bucket4j.distributed.versioning.UsageOfUnsupportedApiException;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;

import java.util.*;

public class SerializationHandles {

    private final Collection<SerializationHandle<?>> allHandles;
    private final SerializationHandle[] handlesById;


    public SerializationHandles(Collection<SerializationHandle<?>> allHandles) {
        Map<Integer, SerializationHandle<?>> serializersById = new HashMap<>();
        int maxTypeId = 0;

        for (SerializationHandle<?> handle : allHandles) {
            int typeId = handle.getTypeId();
            if (typeId <= 0) {
                throw new IllegalArgumentException("non positive typeId=" + typeId + " detected for " + handle);
            }

            maxTypeId = Math.max(maxTypeId, typeId);

            SerializationHandle<?> conflictingHandle = serializersById.get(typeId);
            if (conflictingHandle != null) {
                String msg = "Serialization ID " + typeId + " duplicated for " + handle + " and " + conflictingHandle;
                throw new IllegalArgumentException(msg);
            }
            serializersById.put(typeId, handle);
        }
        this.allHandles = Collections.unmodifiableCollection(allHandles);

        this.handlesById = new SerializationHandle[maxTypeId + 1];
        for (SerializationHandle<?> handle : allHandles) {
            handlesById[handle.getTypeId()] = handle;
        }
    }

    public SerializationHandles merge(SerializationHandle<?>... handles) {
        List<SerializationHandle<?>> resultHandles = new ArrayList<>(this.allHandles);
        for (SerializationHandle<?> handle : handles) {
            resultHandles.add(handle);
        }
        return new SerializationHandles(resultHandles);
    }

    public <T> SerializationHandle<T> getHandleByTypeId(int typeId) {
        if (typeId > 0) {
            if (typeId >= handlesById.length) {
                throw new UnsupportedTypeException(typeId);
            }
            return handlesById[typeId];
        } else {
            typeId = -typeId;
            if (typeId >= PrimitiveSerializationHandles.primitiveHandlesById.length) {
                throw new UnsupportedTypeException(typeId);
            }
            return PrimitiveSerializationHandles.primitiveHandlesById[typeId];
        }
    }

    public Collection<SerializationHandle<?>> getAllHandles() {
        return allHandles;
    }

}
