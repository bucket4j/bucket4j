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

package io.github.bucket4j.distributed.remote.commands;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.MathType;
import io.github.bucket4j.Nothing;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;

import static io.github.bucket4j.distributed.versioning.Versions.v_5_0_0;

public class CreateInitialStateCommand implements RemoteCommand<Nothing>, ComparableByContent<CreateInitialStateCommand> {

    private BucketConfiguration configuration;

    public static SerializationHandle<CreateInitialStateCommand> SERIALIZATION_HANDLE = new SerializationHandle<CreateInitialStateCommand>() {
        @Override
        public <S> CreateInitialStateCommand deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_5_0_0, v_5_0_0);

            BucketConfiguration configuration = BucketConfiguration.SERIALIZATION_HANDLE.deserialize(adapter, input, backwardCompatibilityVersion);

            return new CreateInitialStateCommand(configuration);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, CreateInitialStateCommand command, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeInt(output, v_5_0_0.getNumber());

            BucketConfiguration.SERIALIZATION_HANDLE.serialize(adapter, output, command.configuration, backwardCompatibilityVersion);
        }

        @Override
        public int getTypeId() {
            return 20;
        }

        @Override
        public Class<CreateInitialStateCommand> getSerializedType() {
            return CreateInitialStateCommand.class;
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
        RemoteBucketState remoteBucketState = new RemoteBucketState(configuration, bucketState, new RemoteStat(0L));
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

}
