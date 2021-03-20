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

package io.github.bucket4j.grid;

import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;

public class TryConsumeCommand implements GridCommand<Boolean> {

    private static final long serialVersionUID = 1L;

    private long tokensToConsume;
    private boolean bucketStateModified;

    public static final SerializationHandle<TryConsumeCommand> SERIALIZATION_HANDLE = new SerializationHandle<TryConsumeCommand>() {
        @Override
        public <S> TryConsumeCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            long tokensToConsume = adapter.readLong(input);

            return new TryConsumeCommand(tokensToConsume);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, TryConsumeCommand command) throws IOException {
            adapter.writeLong(output, command.tokensToConsume);
        }

        @Override
        public int getTypeId() {
            return 11;
        }

        @Override
        public Class<TryConsumeCommand> getSerializedType() {
            return TryConsumeCommand.class;
        }

    };

    public TryConsumeCommand(long tokensToConsume) {
        this.tokensToConsume = tokensToConsume;
    }

    @Override
    public Boolean execute(GridBucketState state, long currentTimeNanos) {
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToConsume <= availableToConsume) {
            state.consume(tokensToConsume);
            bucketStateModified = true;
            return true;
        } else {
            bucketStateModified = false;
            return false;
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
