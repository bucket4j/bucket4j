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


import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.Nothing;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;


public class SyncCommand implements RemoteCommand<Nothing>, ComparableByContent<SyncCommand> {

    private final long unsynchronizedTokens;
    private final long nanosSinceLastSync;

    public static final SerializationHandle<SyncCommand> SERIALIZATION_HANDLE = new SerializationHandle<SyncCommand>() {
        @Override
        public <S> SyncCommand deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long unsynchronizedTokens = adapter.readLong(input);
            long nanosSinceLastSync = adapter.readLong(input);
            return new SyncCommand(unsynchronizedTokens, nanosSinceLastSync);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, SyncCommand command, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            adapter.writeLong(output, command.unsynchronizedTokens);
            adapter.writeLong(output, command.nanosSinceLastSync);
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

    public SyncCommand(long unsynchronizedTokens, long nanosSinceLastSync) {
        if (unsynchronizedTokens < 0) {
            throw BucketExceptions.nonPositiveTokensLimitToSync(unsynchronizedTokens);
        }
        if (nanosSinceLastSync < 0) {
            throw BucketExceptions.nonPositiveLimitToSync(nanosSinceLastSync);
        }
        this.unsynchronizedTokens = unsynchronizedTokens;
        this.nanosSinceLastSync = nanosSinceLastSync;
    }

    public long getNanosSinceLastSync() {
        return nanosSinceLastSync;
    }

    public long getUnsynchronizedTokens() {
        return unsynchronizedTokens;
    }

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
        return unsynchronizedTokens == other.unsynchronizedTokens
                && nanosSinceLastSync == other.nanosSinceLastSync;
    }

    @Override
    public boolean isImmediateSyncRequired(long unsynchronizedTokens, long nanosSinceLastSync) {
        return this.unsynchronizedTokens <= unsynchronizedTokens
                && this.nanosSinceLastSync <= nanosSinceLastSync;
    }

    @Override
    public long estimateTokensToConsume() {
        return 0L;
    }

    @Override
    public long getConsumedTokens(Nothing result) {
        return 0L;
    }

}
