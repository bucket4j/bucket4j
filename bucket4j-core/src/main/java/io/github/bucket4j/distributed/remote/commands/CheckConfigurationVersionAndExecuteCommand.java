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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import static io.github.bucket4j.distributed.versioning.Versions.v_8_1_0;

public class CheckConfigurationVersionAndExecuteCommand<T> implements RemoteCommand<T>, ComparableByContent<CheckConfigurationVersionAndExecuteCommand<?>> {

    private final RemoteCommand<T> targetCommand;
    private final long desiredConfigurationVersion;

    public CheckConfigurationVersionAndExecuteCommand(RemoteCommand<T> targetCommand, long desiredConfigurationVersion) {
        this.targetCommand = targetCommand;
        this.desiredConfigurationVersion = desiredConfigurationVersion;
    }

    @Override
    public boolean canBeMerged(RemoteCommand<?> another) {
        if (!(another instanceof CheckConfigurationVersionAndExecuteCommand<?> anotherCmd)) {
            return false;
        }
        return desiredConfigurationVersion == anotherCmd.desiredConfigurationVersion && targetCommand.canBeMerged(anotherCmd.targetCommand);
    }

    @Override
    public RemoteCommand<?> toMergedCommand() {
        return new CheckConfigurationVersionAndExecuteCommand<>(targetCommand.toMergedCommand(), desiredConfigurationVersion);
    }

    @Override
    public void mergeInto(RemoteCommand<?> mergedCommand) {
        targetCommand.mergeInto(((CheckConfigurationVersionAndExecuteCommand) mergedCommand).targetCommand);
    }

    @Override
    public boolean isMerged() {
        return targetCommand.isMerged();
    }

    @Override
    public int getMergedCommandsCount() {
        return targetCommand.getMergedCommandsCount();
    }

    @Override
    public CommandResult<?> unwrapOneResult(T result, int indice) {
        return targetCommand.unwrapOneResult(result, indice);
    }

    public RemoteCommand<T> getTargetCommand() {
        return targetCommand;
    }

    public long getDesiredConfigurationVersion() {
        return desiredConfigurationVersion;
    }

    @Override
    public CommandResult<T> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        Long actualConfigurationVersion = state.getConfigurationVersion();
        if (actualConfigurationVersion == null || actualConfigurationVersion < desiredConfigurationVersion) {
            return CommandResult.configurationNeedToBeReplaced();
        }

        return targetCommand.execute(mutableEntry, currentTimeNanos);
    }

    @Override
    public SerializationHandle<RemoteCommand<?>> getSerializationHandle() {
        return (SerializationHandle) SERIALIZATION_HANDLE;
    }

    public static final SerializationHandle<CheckConfigurationVersionAndExecuteCommand<?>> SERIALIZATION_HANDLE = new SerializationHandle<>() {

        @Override
        public <I> CheckConfigurationVersionAndExecuteCommand<?> deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_8_1_0, v_8_1_0);

            RemoteCommand<?> targetCommand = RemoteCommand.deserialize(adapter, input);
            long desiredConfigurationVersion = adapter.readLong(input);
            return new CheckConfigurationVersionAndExecuteCommand<>(targetCommand, desiredConfigurationVersion);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, CheckConfigurationVersionAndExecuteCommand<?> command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_8_1_0.getNumber());

            RemoteCommand.serialize(adapter, output, command.targetCommand, backwardCompatibilityVersion, scope);
            adapter.writeLong(output, command.desiredConfigurationVersion);
        }

        @Override
        public int getTypeId() {
            return 42;
        }

        @Override
        public Class<CheckConfigurationVersionAndExecuteCommand<?>> getSerializedType() {
            return (Class) CheckConfigurationVersionAndExecuteCommand.class;
        }

        @Override
        public CheckConfigurationVersionAndExecuteCommand<?> fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_8_1_0, v_8_1_0);

            RemoteCommand<?> targetCommand = RemoteCommand.fromJsonCompatibleSnapshot((Map<String, Object>) snapshot.get("targetCommand"));
            long desiredConfigurationVersion = readLongValue(snapshot, "desiredConfigurationVersion");
            return new CheckConfigurationVersionAndExecuteCommand<>(targetCommand, desiredConfigurationVersion);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(CheckConfigurationVersionAndExecuteCommand<?> command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_8_1_0.getNumber());
            result.put("targetCommand", RemoteCommand.toJsonCompatibleSnapshot(command.targetCommand, backwardCompatibilityVersion, scope));
            result.put("desiredConfigurationVersion", command.desiredConfigurationVersion);
            return result;
        }

        @Override
        public String getTypeName() {
            return "CheckConfigurationVersionAndExecuteCommand";
        }
    };

    @Override
    public boolean equalsByContent(CheckConfigurationVersionAndExecuteCommand<?> other) {
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
    public long getConsumedTokens(T result) {
        return targetCommand.getConsumedTokens(result);
    }

    @Override
    public Version getRequiredVersion() {
        return Versions.max(v_8_1_0, targetCommand.getRequiredVersion());
    }

}
