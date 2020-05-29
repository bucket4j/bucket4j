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
package io.github.bucket4j.distributed.remote.commands;

import io.github.bucket4j.VerboseResult;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;

public class VerboseCommand<T> implements RemoteCommand<VerboseResult<T>> {

    private final RemoteCommand<T> targetCommand;

    public VerboseCommand(RemoteCommand<T> targetCommand) {
        this.targetCommand = targetCommand;
    }

    public RemoteCommand<T> getTargetCommand() {
        return targetCommand;
    }

    @Override
    public CommandResult<VerboseResult<T>> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        CommandResult<T> result = targetCommand.execute(mutableEntry, currentTimeNanos);
        VerboseResult<T> verboseResult = new VerboseResult<>(currentTimeNanos, result.getResultTypeId(), result.getData(), state.getConfiguration(), state.getState());
        return CommandResult.success(verboseResult, VerboseResult.SERIALIZATION_HANDLE);
    }

    @Override
    public SerializationHandle<RemoteCommand<?>> getSerializationHandle() {
        return (SerializationHandle) SERIALIZATION_HANDLE;
    }

    public static final SerializationHandle<VerboseCommand<?>> SERIALIZATION_HANDLE = new SerializationHandle<VerboseCommand<?>>() {

        @Override
        public <I> VerboseCommand<?> deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            RemoteCommand<?> targetCommand  = RemoteCommand.deserialize(adapter, input);
            return new VerboseCommand(targetCommand);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, VerboseCommand<?> command) throws IOException {
            RemoteCommand.serialize(adapter, output, command.targetCommand);
        }

        @Override
        public int getTypeId() {
            return 35;
        }

        @Override
        public Class<VerboseCommand<?>> getSerializedType() {
            return (Class) VerboseCommand.class;
        }
    };

}
