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
 * Copyright 2015-2019 Vladimir Bukhtoyarov
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

package io.github.bucket4j.distributed.remote;

import java.io.IOException;
import java.util.Map;

import io.github.bucket4j.distributed.remote.commands.VerboseCommand;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.serialization.SerializationHandles;
import io.github.bucket4j.distributed.versioning.Version;

public interface RemoteCommand<T> {

    CommandResult<T> execute(MutableBucketEntry mutableEntry, long currentTimeNanos);

    default VerboseCommand<T> asVerbose() {
        return VerboseCommand.from(this);
    }

    default boolean isInitializationCommand() {
        return false;
    }

    default boolean canBeMerged(RemoteCommand<?> another) {
        return false;
    }

    default void mergeInto(RemoteCommand<?> mergedCommand) {
        throw new UnsupportedOperationException();
    }

    default RemoteCommand<?> toMergedCommand() {
        throw new UnsupportedOperationException();
    }

    default boolean isMerged() {
        return false;
    }

    default CommandResult<?> unwrapOneResult(T result, int indice) {
        throw new UnsupportedOperationException();
    }

    default int getMergedCommandsCount() {
        throw new UnsupportedOperationException();
    }

    SerializationHandle<RemoteCommand<?>> getSerializationHandle();

    boolean isImmediateSyncRequired(long unsynchronizedTokens, long nanosSinceLastSync);

    long estimateTokensToConsume();

    long getConsumedTokens(T result);

    Version getRequiredVersion();

    static <O> void serialize(SerializationAdapter<O> adapter, O output, RemoteCommand<?> command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
        SerializationHandle<RemoteCommand<?>> serializer = command.getSerializationHandle();
        adapter.writeInt(output, serializer.getTypeId());
        serializer.serialize(adapter, output, command, backwardCompatibilityVersion, scope);
    }

    static <I> RemoteCommand<?> deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
        int typeId = adapter.readInt(input);
        SerializationHandle<?> serializer = SerializationHandles.CORE_HANDLES.getHandleByTypeId(typeId);
        return (RemoteCommand<?>) serializer.deserialize(adapter, input);
    }

    static RemoteCommand<?> fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
        String typeName = (String) snapshot.get("type");
        SerializationHandle<?> serializer = SerializationHandles.CORE_HANDLES.getHandleByTypeName(typeName);
        return (RemoteCommand<?>) serializer.fromJsonCompatibleSnapshot(snapshot);
    }

    static Map<String, Object> toJsonCompatibleSnapshot(RemoteCommand<?> command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
        SerializationHandle<RemoteCommand<?>> serializer = command.getSerializationHandle();
        Map<String, Object> result = command.getSerializationHandle().toJsonCompatibleSnapshot(command, backwardCompatibilityVersion, scope);
        result.put("type", serializer.getTypeName());
        return result;
    }

}
