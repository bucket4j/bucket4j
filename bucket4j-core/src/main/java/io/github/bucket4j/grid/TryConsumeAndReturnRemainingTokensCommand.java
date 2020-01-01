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

package io.github.bucket4j.grid;

import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;


public class TryConsumeAndReturnRemainingTokensCommand implements GridCommand<ConsumptionProbe> {

    private static final long serialVersionUID = 1L;

    private long tokensToConsume;
    private boolean bucketStateModified = false;

    public static SerializationHandle<TryConsumeAndReturnRemainingTokensCommand> SERIALIZATION_HANDLE = new SerializationHandle<TryConsumeAndReturnRemainingTokensCommand>() {
        @Override
        public <S> TryConsumeAndReturnRemainingTokensCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            long tokensToConsume = adapter.readLong(input);

            return new TryConsumeAndReturnRemainingTokensCommand(tokensToConsume);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, TryConsumeAndReturnRemainingTokensCommand command) throws IOException {
            adapter.writeLong(output, command.tokensToConsume);
        }

        @Override
        public int getTypeId() {
            return 12;
        }

        @Override
        public Class<TryConsumeAndReturnRemainingTokensCommand> getSerializedType() {
            return TryConsumeAndReturnRemainingTokensCommand.class;
        }

    };

    public TryConsumeAndReturnRemainingTokensCommand(long tokensToConsume) {
        this.tokensToConsume = tokensToConsume;
    }

    @Override
    public ConsumptionProbe execute(GridBucketState state, long currentTimeNanos) {
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToConsume <= availableToConsume) {
            state.consume(tokensToConsume);
            bucketStateModified = true;
            return ConsumptionProbe.consumed(availableToConsume - tokensToConsume);
        } else {
            long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);
            return ConsumptionProbe.rejected(availableToConsume, nanosToWaitForRefill);
        }
    }

    @Override
    public boolean isBucketStateModified() {
        return bucketStateModified;
    }

    public long getTokensToConsume() {
        return tokensToConsume;
    }

}
