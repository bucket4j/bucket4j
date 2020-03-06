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
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;


public class ReplaceConfigurationOrReturnPreviousCommand implements GridCommand<BucketConfiguration> {

    private static final long serialVersionUID = 8183759647555953907L;

    private BucketConfiguration newConfiguration;
    private boolean replaced;

    public static SerializationHandle<ReplaceConfigurationOrReturnPreviousCommand> SERIALIZATION_HANDLE = new SerializationHandle<ReplaceConfigurationOrReturnPreviousCommand>() {
        @Override
        public <S> ReplaceConfigurationOrReturnPreviousCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            BucketConfiguration newConfiguration = adapter.readObject(input, BucketConfiguration.class);

            return new ReplaceConfigurationOrReturnPreviousCommand(newConfiguration);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ReplaceConfigurationOrReturnPreviousCommand command) throws IOException {
            adapter.writeObject(output, command.newConfiguration);
        }

        @Override
        public int getTypeId() {
            return 13;
        }

        @Override
        public Class<ReplaceConfigurationOrReturnPreviousCommand> getSerializedType() {
            return ReplaceConfigurationOrReturnPreviousCommand.class;
        }

    };

    public ReplaceConfigurationOrReturnPreviousCommand(BucketConfiguration newConfiguration) {
        this.newConfiguration = newConfiguration;
    }

    @Override
    public BucketConfiguration execute(GridBucketState state, long currentTimeNanos) {
        state.refillAllBandwidth(currentTimeNanos);
        BucketConfiguration previousConfiguration = state.replaceConfigurationOrReturnPrevious(newConfiguration);
        if (previousConfiguration != null) {
            return previousConfiguration;
        }
        replaced = true;
        return null;
    }

    @Override
    public boolean isBucketStateModified() {
        return replaced;
    }

    public BucketConfiguration getNewConfiguration() {
        return newConfiguration;
    }

}
