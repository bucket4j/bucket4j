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
package io.github.bucket4j.distributed.serialization;

import io.github.bucket4j.distributed.versioning.UnsupportedTypeException;

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
