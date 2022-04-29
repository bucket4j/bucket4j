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

import io.github.bucket4j.distributed.remote.commands.VerboseCommand;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.serialization.SerializationHandles;
import io.github.bucket4j.distributed.versioning.Version;

import java.io.IOException;

public interface RemoteCommand<T> {

    CommandResult<T> execute(MutableBucketEntry mutableEntry, long currentTimeNanos);

    default VerboseCommand<T> asVerbose() {
        return new VerboseCommand<>(this);
    }

    default boolean isInitializationCommand() {
        return false;
    }

    SerializationHandle<RemoteCommand<?>> getSerializationHandle();

    boolean isImmediateSyncRequired(long unsynchronizedTokens, long nanosSinceLastSync);

    long estimateTokensToConsume();

    long getConsumedTokens(T result);

    static <O> void serialize(SerializationAdapter<O> adapter, O output, RemoteCommand<?> command, Version backwardCompatibilityVersion) throws IOException {
        SerializationHandle<RemoteCommand<?>> serializer = command.getSerializationHandle();
        adapter.writeInt(output, serializer.getTypeId());
        serializer.serialize(adapter, output, command, backwardCompatibilityVersion);
    }

    static <I> RemoteCommand<?> deserialize(DeserializationAdapter<I> adapter, I input, Version backwardCompatibilityVersion) throws IOException {
        int typeId = adapter.readInt(input);
        SerializationHandle<?> serializer = SerializationHandles.CORE_HANDLES.getHandleByTypeId(typeId);
        return (RemoteCommand<?>) serializer.deserialize(adapter, input, backwardCompatibilityVersion);
    }

}
