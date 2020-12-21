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

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Nothing;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;


public class ReplaceConfigurationCommand implements GridCommand<Nothing> {

    private static final long serialVersionUID = 8183759647555953907L;

    private BucketConfiguration newConfiguration;
    private TokensInheritanceStrategy tokensInheritanceStrategy;

    public static final SerializationHandle<ReplaceConfigurationCommand> SERIALIZATION_HANDLE = new SerializationHandle<ReplaceConfigurationCommand>() {
        @Override
        public <S> ReplaceConfigurationCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            BucketConfiguration newConfiguration = adapter.readObject(input, BucketConfiguration.class);
            TokensInheritanceStrategy tokensInheritanceStrategy = TokensInheritanceStrategy.getById(adapter.readByte(input));

            return new ReplaceConfigurationCommand(newConfiguration, tokensInheritanceStrategy);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ReplaceConfigurationCommand command) throws IOException {
            adapter.writeObject(output, command.newConfiguration);
            adapter.writeByte(output, command.tokensInheritanceStrategy.getId());
        }

        @Override
        public int getTypeId() {
            return 13;
        }

        @Override
        public Class<ReplaceConfigurationCommand> getSerializedType() {
            return ReplaceConfigurationCommand.class;
        }

    };

    public ReplaceConfigurationCommand(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        this.newConfiguration = newConfiguration;
        this.tokensInheritanceStrategy = tokensInheritanceStrategy;
    }

    @Override
    public Nothing execute(GridBucketState state, long currentTimeNanos) {
        state.refillAllBandwidth(currentTimeNanos);
        state.replaceConfiguration(newConfiguration, tokensInheritanceStrategy, currentTimeNanos);
        return Nothing.INSTANCE;
    }

    @Override
    public boolean isBucketStateModified() {
        return true;
    }

    public BucketConfiguration getNewConfiguration() {
        return newConfiguration;
    }

    public TokensInheritanceStrategy getTokensInheritanceStrategy() {
        return tokensInheritanceStrategy;
    }

}
