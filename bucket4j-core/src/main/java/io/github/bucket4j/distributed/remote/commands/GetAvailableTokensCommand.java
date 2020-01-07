/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
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


import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;

import static io.github.bucket4j.serialization.PrimitiveSerializationHandles.LONG_HANDLE;

public class GetAvailableTokensCommand implements RemoteCommand<Long> {

    private static final long serialVersionUID = 42;

    public static SerializationHandle<GetAvailableTokensCommand> SERIALIZATION_HANDLE = new SerializationHandle<GetAvailableTokensCommand>() {

        @Override
        public <S> GetAvailableTokensCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            return new GetAvailableTokensCommand();
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, GetAvailableTokensCommand command) throws IOException {
            // do nothing
        }

        @Override
        public int getTypeId() {
            return 9;
        }

        @Override
        public Class<GetAvailableTokensCommand> getSerializedType() {
            return GetAvailableTokensCommand.class;
        }

    };

    @Override
    public CommandResult<Long> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);
        return CommandResult.success(state.getAvailableTokens(), LONG_HANDLE);
    }

    @Override
    public SerializationHandle getSerializationHandle() {
        return SERIALIZATION_HANDLE;
    }
    
}
