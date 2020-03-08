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

import io.github.bucket4j.BucketState;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

public class CreateSnapshotCommand implements GridCommand<BucketState> {

    private static final long serialVersionUID = 1L;

    public static final SerializationHandle<CreateSnapshotCommand> SERIALIZATION_HANDLE = new SerializationHandle<CreateSnapshotCommand>() {
        @Override
        public <S> CreateSnapshotCommand deserialize(DeserializationAdapter<S> adapter, S input) {
            return new CreateSnapshotCommand();
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, CreateSnapshotCommand command) {
            // do nothing
        }

        @Override
        public int getTypeId() {
            return 8;
        }

        @Override
        public Class<CreateSnapshotCommand> getSerializedType() {
            return CreateSnapshotCommand.class;
        }

    };

    @Override
    public BucketState execute(GridBucketState gridState, long currentTimeNanos) {
        return gridState.copyBucketState();
    }

    @Override
    public boolean isBucketStateModified() {
        return false;
    }

}
