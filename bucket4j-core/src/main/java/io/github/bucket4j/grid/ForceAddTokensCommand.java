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

import io.github.bucket4j.Nothing;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;

public class ForceAddTokensCommand implements GridCommand<Nothing> {

    private static final long serialVersionUID = 1L;

    private long tokensToAdd;

    public static final SerializationHandle<ForceAddTokensCommand> SERIALIZATION_HANDLE = new SerializationHandle<ForceAddTokensCommand>() {
        @Override
        public <S> ForceAddTokensCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            long tokensToAdd = adapter.readLong(input);

            return new ForceAddTokensCommand(tokensToAdd);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ForceAddTokensCommand command) throws IOException {
            adapter.writeLong(output, command.tokensToAdd);
        }

        @Override
        public int getTypeId() {
            return 25;
        }

        @Override
        public Class<ForceAddTokensCommand> getSerializedType() {
            return ForceAddTokensCommand.class;
        }

    };

    public ForceAddTokensCommand(long tokensToAdd) {
        this.tokensToAdd = tokensToAdd;
    }

    @Override
    public Nothing execute(GridBucketState state, long currentTimeNanos) {
        state.refillAllBandwidth(currentTimeNanos);
        state.addTokens(tokensToAdd);
        return Nothing.INSTANCE;
    }

    @Override
    public boolean isBucketStateModified() {
        return true;
    }

    public long getTokensToAdd() {
        return tokensToAdd;
    }

}
