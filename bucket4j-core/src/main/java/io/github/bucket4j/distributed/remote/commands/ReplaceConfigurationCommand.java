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
import io.github.bucket4j.Nothing;
import io.github.bucket4j.TokensInheritanceStrategy;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;


public class ReplaceConfigurationCommand implements RemoteCommand<Nothing>, ComparableByContent<ReplaceConfigurationCommand> {

    private final TokensInheritanceStrategy tokensInheritanceStrategy;
    private final BucketConfiguration newConfiguration;

    public static final SerializationHandle<ReplaceConfigurationCommand> SERIALIZATION_HANDLE = new SerializationHandle<ReplaceConfigurationCommand>() {
        @Override
        public <S> ReplaceConfigurationCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            BucketConfiguration newConfiguration = BucketConfiguration.SERIALIZATION_HANDLE.deserialize(adapter, input);
            TokensInheritanceStrategy tokensInheritanceStrategy = TokensInheritanceStrategy.getById(adapter.readByte(input));
            return new ReplaceConfigurationCommand(newConfiguration, tokensInheritanceStrategy);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ReplaceConfigurationCommand command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            BucketConfiguration.SERIALIZATION_HANDLE.serialize(adapter, output, command.newConfiguration, backwardCompatibilityVersion, scope);
            adapter.writeByte(output, command.tokensInheritanceStrategy.getId());
        }

        @Override
        public int getTypeId() {
            return 32;
        }

        @Override
        public Class<ReplaceConfigurationCommand> getSerializedType() {
            return ReplaceConfigurationCommand.class;
        }

        @Override
        public ReplaceConfigurationCommand fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            TokensInheritanceStrategy tokensInheritanceStrategy = TokensInheritanceStrategy.valueOf((String) snapshot.get("tokensInheritanceStrategy"));
            BucketConfiguration newConfiguration = BucketConfiguration.SERIALIZATION_HANDLE
                    .fromJsonCompatibleSnapshot((Map<String, Object>) snapshot.get("newConfiguration"));
            return new ReplaceConfigurationCommand(newConfiguration, tokensInheritanceStrategy);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(ReplaceConfigurationCommand command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());
            result.put("tokensInheritanceStrategy", command.tokensInheritanceStrategy.toString());
            result.put("newConfiguration", BucketConfiguration.SERIALIZATION_HANDLE.toJsonCompatibleSnapshot(command.newConfiguration, backwardCompatibilityVersion, scope));
            return result;
        }

        @Override
        public String getTypeName() {
            return "ReplaceConfigurationCommand";
        }

    };

    public ReplaceConfigurationCommand(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        this.newConfiguration = newConfiguration;
        this.tokensInheritanceStrategy = tokensInheritanceStrategy;
    }

    @Override
    public CommandResult<Nothing> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);
        state.replaceConfiguration(newConfiguration, tokensInheritanceStrategy, currentTimeNanos);
        mutableEntry.set(state);
        return CommandResult.empty();
    }

    public BucketConfiguration getNewConfiguration() {
        return newConfiguration;
    }

    public TokensInheritanceStrategy getTokensInheritanceStrategy() {
        return tokensInheritanceStrategy;
    }

    @Override
    public SerializationHandle getSerializationHandle() {
        return SERIALIZATION_HANDLE;
    }

    @Override
    public boolean equalsByContent(ReplaceConfigurationCommand other) {
        return ComparableByContent.equals(newConfiguration, other.newConfiguration) &&
                tokensInheritanceStrategy == other.tokensInheritanceStrategy;
    }

    @Override
    public boolean isImmediateSyncRequired(long unsynchronizedTokens, long nanosSinceLastSync) {
        return true;
    }

    @Override
    public long estimateTokensToConsume() {
        return 0;
    }

    @Override
    public long getConsumedTokens(Nothing result) {
        return 0;
    }

    @Override
    public Version getRequiredVersion() {
        return v_7_0_0;
    }

}
