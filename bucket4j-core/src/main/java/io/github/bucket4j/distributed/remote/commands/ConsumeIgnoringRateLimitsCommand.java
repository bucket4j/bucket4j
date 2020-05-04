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
/*
 *
 * Copyright 2015-2020 Vladimir Bukhtoyarov
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

public class ConsumeIgnoringRateLimitsCommand implements RemoteCommand<Long> {

    private long tokensToConsume;

    public static final SerializationHandle<ConsumeIgnoringRateLimitsCommand> SERIALIZATION_HANDLE = new SerializationHandle<ConsumeIgnoringRateLimitsCommand>() {
        @Override
        public <S> ConsumeIgnoringRateLimitsCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            long tokensToConsume = adapter.readLong(input);

            return new ConsumeIgnoringRateLimitsCommand(tokensToConsume);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ConsumeIgnoringRateLimitsCommand command) throws IOException {
            adapter.writeLong(output, command.tokensToConsume);
        }

        @Override
        public int getTypeId() {
            return 34;
        }

        @Override
        public Class<ConsumeIgnoringRateLimitsCommand> getSerializedType() {
            return ConsumeIgnoringRateLimitsCommand.class;
        }

    };

    public ConsumeIgnoringRateLimitsCommand(long limit) {
        this.tokensToConsume = limit;
    }

    public long getTokensToConsume() {
        return tokensToConsume;
    }

    @Override
    public CommandResult<Long> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);
        long nanosToCloseDeficit = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);

        if (nanosToCloseDeficit == Long.MAX_VALUE) {
            return CommandResult.success(Long.MAX_VALUE, LONG_HANDLE);
        }
        state.consume(tokensToConsume);
        return CommandResult.success(nanosToCloseDeficit, LONG_HANDLE);
    }

    @Override
    public SerializationHandle<RemoteCommand<?>> getSerializationHandle() {
        return (SerializationHandle) SERIALIZATION_HANDLE;
    }

}
