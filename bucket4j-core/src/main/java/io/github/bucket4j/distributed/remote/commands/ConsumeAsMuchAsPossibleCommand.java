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
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;

import static io.github.bucket4j.serialization.PrimitiveSerializationHandles.LONG_HANDLE;

public class ConsumeAsMuchAsPossibleCommand implements RemoteCommand<Long>, ComparableByContent<ConsumeAsMuchAsPossibleCommand> {

    private long limit;

    public static SerializationHandle<ConsumeAsMuchAsPossibleCommand> SERIALIZATION_HANDLE = new SerializationHandle<ConsumeAsMuchAsPossibleCommand>() {
        @Override
        public <S> ConsumeAsMuchAsPossibleCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            long limit = adapter.readLong(input);

            return new ConsumeAsMuchAsPossibleCommand(limit);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ConsumeAsMuchAsPossibleCommand command) throws IOException {
            adapter.writeLong(output, command.limit);
        }

        @Override
        public int getTypeId() {
            return 25;
        }

        @Override
        public Class<ConsumeAsMuchAsPossibleCommand> getSerializedType() {
            return ConsumeAsMuchAsPossibleCommand.class;
        }

    };

    public ConsumeAsMuchAsPossibleCommand(long limit) {
        this.limit = limit;
    }

    @Override
    public CommandResult<Long> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        long toConsume = Math.min(limit, availableToConsume);
        if (toConsume <= 0) {
            return CommandResult.ZERO;
        }
        state.consume(toConsume);
        mutableEntry.set(state);
        return CommandResult.success(toConsume, LONG_HANDLE);
    }

    public long getLimit() {
        return limit;
    }

    @Override
    public SerializationHandle getSerializationHandle() {
        return SERIALIZATION_HANDLE;
    }

    @Override
    public boolean equalsByContent(ConsumeAsMuchAsPossibleCommand other) {
        return limit == other.limit;
    }

}
