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

import io.github.bucket4j.*;
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

public class CreateInitialStateCommand implements RemoteCommand<Nothing>, ComparableByContent<CreateInitialStateCommand> {

    private BucketConfiguration configuration;

    public static SerializationHandle<CreateInitialStateCommand> SERIALIZATION_HANDLE = new SerializationHandle<CreateInitialStateCommand>() {
        @Override
        public <S> CreateInitialStateCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            BucketConfiguration configuration = BucketConfiguration.SERIALIZATION_HANDLE.deserialize(adapter, input);

            return new CreateInitialStateCommand(configuration);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, CreateInitialStateCommand command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            BucketConfiguration.SERIALIZATION_HANDLE.serialize(adapter, output, command.configuration, backwardCompatibilityVersion, scope);
        }

        @Override
        public int getTypeId() {
            return 20;
        }

        @Override
        public Class<CreateInitialStateCommand> getSerializedType() {
            return CreateInitialStateCommand.class;
        }

        @Override
        public CreateInitialStateCommand fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            BucketConfiguration configuration = BucketConfiguration.SERIALIZATION_HANDLE
                    .fromJsonCompatibleSnapshot((Map<String, Object>) snapshot.get("configuration"));
            return new CreateInitialStateCommand(configuration);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(CreateInitialStateCommand command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());
            result.put("configuration", BucketConfiguration.SERIALIZATION_HANDLE.toJsonCompatibleSnapshot(command.configuration, backwardCompatibilityVersion, scope));
            return result;
        }

        @Override
        public String getTypeName() {
            return "CreateInitialStateCommand";
        }

    };

    public CreateInitialStateCommand(BucketConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public CommandResult<Nothing> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (mutableEntry.exists()) {
            return CommandResult.NOTHING;
        }

        BucketState bucketState = BucketState.createInitialState(configuration, MathType.INTEGER_64_BITS, currentTimeNanos);
        RemoteBucketState remoteBucketState = new RemoteBucketState(bucketState, new RemoteStat(0L), null);
        mutableEntry.set(remoteBucketState);
        return CommandResult.NOTHING;
    }

    @Override
    public boolean isInitializationCommand() {
        return true;
    }

    public BucketConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public SerializationHandle getSerializationHandle() {
        return SERIALIZATION_HANDLE;
    }

    @Override
    public boolean equalsByContent(CreateInitialStateCommand other) {
        return ComparableByContent.equals(configuration, other.configuration);
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
