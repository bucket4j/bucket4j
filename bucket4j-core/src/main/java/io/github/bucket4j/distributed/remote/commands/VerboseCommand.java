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

import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class VerboseCommand<T> implements RemoteCommand<RemoteVerboseResult<T>>, ComparableByContent<VerboseCommand<?>> {

    private final RemoteCommand<T> targetCommand;

    public VerboseCommand(RemoteCommand<T> targetCommand) {
        this.targetCommand = targetCommand;
    }

    public RemoteCommand<T> getTargetCommand() {
        return targetCommand;
    }

    @Override
    public CommandResult<RemoteVerboseResult<T>> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        CommandResult<T> result = targetCommand.execute(mutableEntry, currentTimeNanos);
        RemoteVerboseResult<T> verboseResult = new RemoteVerboseResult<>(currentTimeNanos, result.getResultTypeId(), result.getData(), state);
        return CommandResult.success(verboseResult, RemoteVerboseResult.SERIALIZATION_HANDLE);
    }

    @Override
    public SerializationHandle<RemoteCommand<?>> getSerializationHandle() {
        return (SerializationHandle) SERIALIZATION_HANDLE;
    }

    public static final SerializationHandle<VerboseCommand<?>> SERIALIZATION_HANDLE = new SerializationHandle<VerboseCommand<?>>() {

        @Override
        public <I> VerboseCommand<?> deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            RemoteCommand<?> targetCommand  = RemoteCommand.deserialize(adapter, input);
            return new VerboseCommand<>(targetCommand);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, VerboseCommand<?> command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            RemoteCommand.serialize(adapter, output, command.targetCommand, backwardCompatibilityVersion, scope);
        }

        @Override
        public int getTypeId() {
            return 35;
        }

        @Override
        public Class<VerboseCommand<?>> getSerializedType() {
            return (Class) VerboseCommand.class;
        }

        @Override
        public VerboseCommand<?> fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            RemoteCommand<?> targetCommand = RemoteCommand.fromJsonCompatibleSnapshot((Map<String, Object>) snapshot.get("targetCommand"));
            return new VerboseCommand<>(targetCommand);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(VerboseCommand<?> command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());
            result.put("targetCommand", RemoteCommand.toJsonCompatibleSnapshot(command.targetCommand, backwardCompatibilityVersion, scope));
            return result;
        }

        @Override
        public String getTypeName() {
            return "VerboseCommand";
        }
    };

    @Override
    public boolean equalsByContent(VerboseCommand<?> other) {
        return ComparableByContent.equals(targetCommand, other.targetCommand);
    }

    @Override
    public boolean isImmediateSyncRequired(long unsynchronizedTokens, long nanosSinceLastSync) {
        return targetCommand.isImmediateSyncRequired(unsynchronizedTokens, nanosSinceLastSync);
    }

    @Override
    public long estimateTokensToConsume() {
        return targetCommand.estimateTokensToConsume();
    }

    @Override
    public long getConsumedTokens(RemoteVerboseResult<T> result) {
        return targetCommand.getConsumedTokens(result.getValue());
    }

}
