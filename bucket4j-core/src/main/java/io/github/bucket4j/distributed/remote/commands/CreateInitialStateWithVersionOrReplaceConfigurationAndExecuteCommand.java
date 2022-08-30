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

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.MathType;
import io.github.bucket4j.TokensInheritanceStrategy;
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

import static io.github.bucket4j.distributed.versioning.Versions.v_8_1_0;

public class CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<T> implements RemoteCommand<T>, ComparableByContent<CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand> {

    private RemoteCommand<T> targetCommand;
    private BucketConfiguration configuration;
    private long desiredConfigurationVersion;
    private TokensInheritanceStrategy tokensInheritanceStrategy;

    public static SerializationHandle<CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<?>> SERIALIZATION_HANDLE = new SerializationHandle<CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<?>>() {
        @Override
        public <S> CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<?> deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_8_1_0, v_8_1_0);

            BucketConfiguration configuration = BucketConfiguration.SERIALIZATION_HANDLE.deserialize(adapter, input);
            RemoteCommand<?> targetCommand = RemoteCommand.deserialize(adapter, input);
            long desiredConfigurationVersion = adapter.readLong(input);
            TokensInheritanceStrategy tokensInheritanceStrategy = TokensInheritanceStrategy.getById(adapter.readByte(input));

            return new CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<>(configuration, targetCommand, desiredConfigurationVersion, tokensInheritanceStrategy);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<?> command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_8_1_0.getNumber());

            BucketConfiguration.SERIALIZATION_HANDLE.serialize(adapter, output, command.configuration, backwardCompatibilityVersion, scope);
            RemoteCommand.serialize(adapter, output, command.targetCommand, backwardCompatibilityVersion, scope);
            adapter.writeLong(output, command.desiredConfigurationVersion);
            adapter.writeByte(output, command.tokensInheritanceStrategy.getId());
        }

        @Override
        public int getTypeId() {
            return 41;
        }

        @Override
        public Class<CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<?>> getSerializedType() {
            return (Class) CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand.class;
        }

        @Override
        public CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<?> fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_8_1_0, v_8_1_0);

            BucketConfiguration configuration = BucketConfiguration.SERIALIZATION_HANDLE
                    .fromJsonCompatibleSnapshot((Map<String, Object>) snapshot.get("configuration"));
            RemoteCommand<?> targetCommand = RemoteCommand.fromJsonCompatibleSnapshot((Map<String, Object>) snapshot.get("targetCommand"));
            TokensInheritanceStrategy tokensInheritanceStrategy = TokensInheritanceStrategy.valueOf((String) snapshot.get("tokensInheritanceStrategy"));
            long desiredConfigurationVersion = readLongValue(snapshot, "desiredConfigurationVersion");
            return new CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<>(configuration, targetCommand, desiredConfigurationVersion, tokensInheritanceStrategy);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<?> command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_8_1_0.getNumber());
            result.put("configuration", BucketConfiguration.SERIALIZATION_HANDLE.toJsonCompatibleSnapshot(command.configuration, backwardCompatibilityVersion, scope));
            result.put("targetCommand", RemoteCommand.toJsonCompatibleSnapshot(command.targetCommand, backwardCompatibilityVersion, scope));
            result.put("desiredConfigurationVersion", command.desiredConfigurationVersion);
            result.put("tokensInheritanceStrategy", command.tokensInheritanceStrategy.toString());

            return result;
        }

        @Override
        public String getTypeName() {
            return "CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand";
        }

    };

    public CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand(BucketConfiguration configuration, RemoteCommand<T> targetCommand, long desiredConfigurationVersion, TokensInheritanceStrategy tokensInheritanceStrategy) {
        this.configuration = configuration;
        this.targetCommand = targetCommand;
        this.desiredConfigurationVersion = desiredConfigurationVersion;
        this.tokensInheritanceStrategy = tokensInheritanceStrategy;
    }

    @Override
    public CommandResult<T> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        BucketEntryWrapper entryWrapper;
        if (mutableEntry.exists()) {
            RemoteBucketState state = mutableEntry.get();
            entryWrapper = new BucketEntryWrapper(state);
            Long actualConfigurationVersion = state.getConfigurationVersion();
            if (actualConfigurationVersion == null || actualConfigurationVersion < desiredConfigurationVersion) {
                ReplaceConfigurationCommand replaceConfigurationCommand = new ReplaceConfigurationCommand(configuration, tokensInheritanceStrategy);
                replaceConfigurationCommand.execute(entryWrapper, currentTimeNanos);
                state.setConfigurationVersion(desiredConfigurationVersion);
            }
        } else {
            BucketState bucketState = BucketState.createInitialState(configuration, MathType.INTEGER_64_BITS, currentTimeNanos);
            RemoteBucketState state = new RemoteBucketState(bucketState, new RemoteStat(0), desiredConfigurationVersion);
            entryWrapper = new BucketEntryWrapper(state);
            entryWrapper.setStateModified(true);
        }

        CommandResult<T> result = targetCommand.execute(entryWrapper, currentTimeNanos);
        if (entryWrapper.isStateModified()) {
            mutableEntry.set(entryWrapper.get());
        }
        mutableEntry.set(entryWrapper.get());
        return result;
    }

    public BucketConfiguration getConfiguration() {
        return configuration;
    }

    public RemoteCommand<T> getTargetCommand() {
        return targetCommand;
    }

    public long getDesiredConfigurationVersion() {
        return desiredConfigurationVersion;
    }

    public TokensInheritanceStrategy getTokensInheritanceStrategy() {
        return tokensInheritanceStrategy;
    }

    @Override
    public boolean isInitializationCommand() {
        return true;
    }

    @Override
    public SerializationHandle getSerializationHandle() {
        return SERIALIZATION_HANDLE;
    }

    @Override
    public boolean equalsByContent(CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand other) {
        return ComparableByContent.equals(configuration, other.configuration) &&
                ComparableByContent.equals(targetCommand, other.targetCommand) &&
                desiredConfigurationVersion == other.desiredConfigurationVersion &&
                tokensInheritanceStrategy == other.tokensInheritanceStrategy;
    }

    @Override
    public boolean isImmediateSyncRequired(long unsynchronizedTokens, long nanosSinceLastSync) {
        return true;
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
