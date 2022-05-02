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
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class AddTokensCommand implements RemoteCommand<Nothing>, ComparableByContent<AddTokensCommand> {

    private long tokensToAdd;

    public static final SerializationHandle<AddTokensCommand> SERIALIZATION_HANDLE = new SerializationHandle<AddTokensCommand>() {
        @Override
        public <S> AddTokensCommand deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long tokensToAdd = adapter.readLong(input);

            return new AddTokensCommand(tokensToAdd);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, AddTokensCommand command, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            adapter.writeLong(output, command.tokensToAdd);
        }

        @Override
        public int getTypeId() {
            return 24;
        }

        @Override
        public Class<AddTokensCommand> getSerializedType() {
            return AddTokensCommand.class;
        }

        @Override
        public AddTokensCommand fromJsonCompatibleSnapshot(Map<String, Object> snapshot, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long tokensToAdd = readLongValue(snapshot, "tokensToAdd");
            return new AddTokensCommand(tokensToAdd);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(AddTokensCommand command, Version backwardCompatibilityVersion) throws IOException {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());
            result.put("tokensToAdd", command.tokensToAdd);
            return result;
        }

        @Override
        public String getTypeName() {
            return "AddTokensCommand";
        }

    };

    public AddTokensCommand(long tokensToAdd) {
        this.tokensToAdd = tokensToAdd;
    }

    @Override
    public CommandResult<Nothing> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }
        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);
        state.addTokens(tokensToAdd);
        mutableEntry.set(state);
        return CommandResult.NOTHING;
    }

    public long getTokensToAdd() {
        return tokensToAdd;
    }

    @Override
    public SerializationHandle getSerializationHandle() {
        return SERIALIZATION_HANDLE;
    }

    @Override
    public boolean equalsByContent(AddTokensCommand other) {
        return tokensToAdd == other.tokensToAdd;
    }

    @Override
    public boolean isImmediateSyncRequired(long unsynchronizedTokens, long nanosSinceLastSync) {
        return true;
    }

    @Override
    public long estimateTokensToConsume() {
        return 0;
    }

    @Override
    public long getConsumedTokens(Nothing result) {
        return 0;
    }

}
