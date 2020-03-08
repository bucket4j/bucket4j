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

import io.github.bucket4j.VerboseResult;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;
import java.io.Serializable;

public class VerboseCommand<T extends Serializable> implements GridCommand<VerboseResult<T>> {

    private final GridCommand<T> targetCommand;

    public VerboseCommand(GridCommand<T> targetCommand) {
        this.targetCommand = targetCommand;
    }

    @Override
    public VerboseResult<T> execute(GridBucketState state, long currentTimeNanos) {
        T result = targetCommand.execute(state, currentTimeNanos);
        return new VerboseResult<>(currentTimeNanos, result, state.getConfiguration(), state.getState());
    }

    @Override
    public boolean isBucketStateModified() {
        return targetCommand.isBucketStateModified();
    }

    public GridCommand<T> getTargetCommand() {
        return targetCommand;
    }

    public static final SerializationHandle<VerboseCommand<?>> SERIALIZATION_HANDLE = new SerializationHandle<VerboseCommand<?>>() {

        @Override
        public <I> VerboseCommand<?> deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            GridCommand<?> targetCommand  = (GridCommand<?>) adapter.readObject(input);
            return new VerboseCommand<>(targetCommand);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, VerboseCommand<?> command) throws IOException {
            adapter.writeObject(output, command.targetCommand);
        }

        @Override
        public int getTypeId() {
            return 25;
        }

        @Override
        public Class<VerboseCommand<?>> getSerializedType() {
            return (Class) VerboseCommand.class;
        }
    };

}
