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

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.remote.commands.*;
import io.github.bucket4j.distributed.versioning.UnsupportedNamedTypeException;
import io.github.bucket4j.distributed.versioning.UnsupportedTypeException;
import io.github.bucket4j.local.LockFreeBucket;
import io.github.bucket4j.local.SynchronizedBucket;
import io.github.bucket4j.local.ThreadUnsafeBucket;

import java.util.*;

public class SerializationHandles {

    public static final SerializationHandles CORE_HANDLES = new SerializationHandles(Arrays.asList(
            Bandwidth.SERIALIZATION_HANDLE, // 1
            BucketConfiguration.SERIALIZATION_HANDLE, // 2
            BucketState64BitsInteger.SERIALIZATION_HANDLE, // 3
//            BucketStateIEEE754.SERIALIZATION_HANDLE, // 4
            RemoteBucketState.SERIALIZATION_HANDLE, // 5
            RemoteStat.SERIALIZATION_HANDLE, // 6

            CommandResult.SERIALIZATION_HANDLE, // 10
            ConsumptionProbe.SERIALIZATION_HANDLE, // 11
            EstimationProbe.SERIALIZATION_HANDLE, // 12
            MultiResult.SERIALIZATION_HANDLE, // 13
            RemoteVerboseResult.SERIALIZATION_HANDLE, // 14
            BucketNotFoundError.SERIALIZATION_HANDLE, // 15
            UnsupportedTypeError.SERIALIZATION_HANDLE, // 16
            UsageOfObsoleteApiError.SERIALIZATION_HANDLE, // 17
            UsageOfUnsupportedApiError.SERIALIZATION_HANDLE, // 18
            UnsupportedNamedTypeError.SERIALIZATION_HANDLE, // 19

            CreateInitialStateAndExecuteCommand.SERIALIZATION_HANDLE, // 21
            MultiCommand.SERIALIZATION_HANDLE, // 22
            ReserveAndCalculateTimeToSleepCommand.SERIALIZATION_HANDLE, // 23
            AddTokensCommand.SERIALIZATION_HANDLE, // 24
            ConsumeAsMuchAsPossibleCommand.SERIALIZATION_HANDLE, // 25
            CreateSnapshotCommand.SERIALIZATION_HANDLE, // 26
            GetAvailableTokensCommand.SERIALIZATION_HANDLE, // 27
            EstimateAbilityToConsumeCommand.SERIALIZATION_HANDLE, // 28
            TryConsumeCommand.SERIALIZATION_HANDLE, // 29
            TryConsumeAndReturnRemainingTokensCommand.SERIALIZATION_HANDLE, // 30
            ReplaceConfigurationCommand.SERIALIZATION_HANDLE, // 32
            GetConfigurationCommand.SERIALIZATION_HANDLE, // 33
            ConsumeIgnoringRateLimitsCommand.SERIALIZATION_HANDLE, // 34
            VerboseCommand.SERIALIZATION_HANDLE, // 35
            SyncCommand.SERIALIZATION_HANDLE, // 36
            Request.SERIALIZATION_HANDLE, // 37
            ForceAddTokensCommand.SERIALIZATION_HANDLE, // 38
            ResetCommand.SERIALIZATION_HANDLE, // 39
            ConfigurationNeedToBeReplacedError.SERIALIZATION_HANDLE, // 40
            CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand.SERIALIZATION_HANDLE, // 41
            CheckConfigurationVersionAndExecuteCommand.SERIALIZATION_HANDLE, // 42

            LockFreeBucket.SERIALIZATION_HANDLE, // 60
            SynchronizedBucket.SERIALIZATION_HANDLE, // 61
            ThreadUnsafeBucket.SERIALIZATION_HANDLE // 62
    ));

    private final Collection<SerializationHandle<?>> allHandles;
    private final SerializationHandle[] handlesById;
    private final Map<String, SerializationHandle<?>> handlesByName;


    public SerializationHandles(Collection<SerializationHandle<?>> allHandles) {
        this.handlesByName = new HashMap<>();

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

            String typeName = handle.getTypeName();
            if (typeName == null || typeName.isEmpty()) {
                throw new IllegalArgumentException("null typeName detected for " + handle);
            }
            conflictingHandle = handlesByName.get(typeName);
            if (conflictingHandle != null) {
                String msg = "Serialization typeName " + typeName + " duplicated for " + handle + " and " + conflictingHandle;
                throw new IllegalArgumentException(msg);
            }
            handlesByName.put(typeName, handle);
        }

        for (SerializationHandle<?> handle : PrimitiveSerializationHandles.primitiveHandlesById) {
            String typeName = handle.getTypeName();
            if (typeName == null || typeName.isEmpty()) {
                throw new IllegalArgumentException("null typeName detected for " + handle);
            }
            SerializationHandle<?> conflictingHandle = handlesByName.get(typeName);
            if (conflictingHandle != null) {
                String msg = "Serialization typeName " + typeName + " duplicated for " + handle + " and " + conflictingHandle;
                throw new IllegalArgumentException(msg);
            }
            handlesByName.put(typeName, handle);
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
            SerializationHandle<T> serializationHandle = handlesById[typeId];
            if (serializationHandle == null) {
                throw new UnsupportedTypeException(typeId);
            }
            return serializationHandle;
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

    public SerializationHandle<?> getHandleByTypeName(String typeName) {
        SerializationHandle<?> handle = handlesByName.get(typeName);
        if (handle == null) {
            throw new UnsupportedNamedTypeException(typeName);
        }
        return handle;
    }

}
