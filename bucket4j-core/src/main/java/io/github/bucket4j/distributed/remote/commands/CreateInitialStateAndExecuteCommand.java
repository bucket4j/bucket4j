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
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class CreateInitialStateAndExecuteCommand<T> implements RemoteCommand<T>, ComparableByContent<CreateInitialStateAndExecuteCommand> {

    private RemoteCommand<T> targetCommand;
    private BucketConfiguration configuration;

    public static SerializationHandle<CreateInitialStateAndExecuteCommand> SERIALIZATION_HANDLE = new SerializationHandle<CreateInitialStateAndExecuteCommand>() {
        @Override
        public <S> CreateInitialStateAndExecuteCommand deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            BucketConfiguration configuration = BucketConfiguration.SERIALIZATION_HANDLE.deserialize(adapter, input, backwardCompatibilityVersion);
            RemoteCommand<?> targetCommand = RemoteCommand.deserialize(adapter, input, backwardCompatibilityVersion);

            return new CreateInitialStateAndExecuteCommand(configuration, targetCommand);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, CreateInitialStateAndExecuteCommand command, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            BucketConfiguration.SERIALIZATION_HANDLE.serialize(adapter, output, command.configuration, backwardCompatibilityVersion);
            RemoteCommand.serialize(adapter, output, command.targetCommand, backwardCompatibilityVersion);
        }

        @Override
        public int getTypeId() {
            return 21;
        }

        @Override
        public Class<CreateInitialStateAndExecuteCommand> getSerializedType() {
            return CreateInitialStateAndExecuteCommand.class;
        }

    };

    public CreateInitialStateAndExecuteCommand(BucketConfiguration configuration, RemoteCommand<T> targetCommand) {
        this.configuration = configuration;
        this.targetCommand = targetCommand;
    }

    @Override
    public CommandResult<T> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        RemoteBucketState state;
        if (mutableEntry.exists()) {
            state = mutableEntry.get();
        } else {
            BucketState bucketState = BucketState.createInitialState(configuration, MathType.INTEGER_64_BITS, currentTimeNanos);
            state = new RemoteBucketState(configuration, bucketState, new RemoteStat(0));
        }

        BucketEntryWrapper entryWrapper = new BucketEntryWrapper(state);
        CommandResult<T> result = targetCommand.execute(entryWrapper, currentTimeNanos);
        mutableEntry.set(entryWrapper.get());
        return result;
    }

    public BucketConfiguration getConfiguration() {
        return configuration;
    }

    public RemoteCommand<T> getTargetCommand() {
        return targetCommand;
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
    public boolean equalsByContent(CreateInitialStateAndExecuteCommand other) {
        return ComparableByContent.equals(configuration, other.configuration) &&
                ComparableByContent.equals(targetCommand, other.targetCommand);
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

    private static class BucketEntryWrapper implements MutableBucketEntry {

        private RemoteBucketState state;

        public BucketEntryWrapper(RemoteBucketState state) {
            this.state = state;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public void set(RemoteBucketState state) {
            this.state = state;
        }

        @Override
        public RemoteBucketState get() {
            return state;
        }
    }

}
