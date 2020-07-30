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

package io.github.bucket4j.distributed.remote.commands;


import io.github.bucket4j.Nothing;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.util.ComparableByContent;


public class SyncCommand implements RemoteCommand<Nothing>, ComparableByContent<SyncCommand> {

    public static final SerializationHandle<SyncCommand> SERIALIZATION_HANDLE = new SerializationHandle<SyncCommand>() {
        @Override
        public <S> SyncCommand deserialize(DeserializationAdapter<S> adapter, S input) {
            return new SyncCommand();
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, SyncCommand command) {
            // do nothing
        }

        @Override
        public int getTypeId() {
            return 36;
        }

        @Override
        public Class<SyncCommand> getSerializedType() {
            return SyncCommand.class;
        }

    };

    @Override
    public CommandResult<Nothing> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }
        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);
        mutableEntry.set(state);
        return CommandResult.NOTHING;
    }

    @Override
    public SerializationHandle getSerializationHandle() {
        return SERIALIZATION_HANDLE;
    }

    @Override
    public boolean equalsByContent(SyncCommand other) {
        return true;
    }

}
