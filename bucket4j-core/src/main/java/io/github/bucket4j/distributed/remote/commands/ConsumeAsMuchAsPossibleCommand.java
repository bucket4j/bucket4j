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

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;

import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.github.bucket4j.distributed.serialization.PrimitiveSerializationHandles.BOOLEAN_HANDLE;
import static io.github.bucket4j.distributed.serialization.PrimitiveSerializationHandles.LONG_HANDLE;
import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class ConsumeAsMuchAsPossibleCommand implements RemoteCommand<Long>, ComparableByContent<ConsumeAsMuchAsPossibleCommand> {

    private long limit;
    private boolean merged;

    public static final SerializationHandle<ConsumeAsMuchAsPossibleCommand> SERIALIZATION_HANDLE = new SerializationHandle<>() {
        @Override
        public <S> ConsumeAsMuchAsPossibleCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long limit = adapter.readLong(input);

            return new ConsumeAsMuchAsPossibleCommand(limit);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ConsumeAsMuchAsPossibleCommand command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            adapter.writeLong(output, command.limit);
        }

        @Override
        public int getTypeId() {
            return 25;
        }

        @Override
        public Class<ConsumeAsMuchAsPossibleCommand> getSerializedType() {
            return ConsumeAsMuchAsPossibleCommand.class;
        }

        @Override
        public ConsumeAsMuchAsPossibleCommand fromJsonCompatibleSnapshot(Map<String, Object> snapshot) {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long limit = readLongValue(snapshot, "limit");
            return new ConsumeAsMuchAsPossibleCommand(limit);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(ConsumeAsMuchAsPossibleCommand command, Version backwardCompatibilityVersion, Scope scope) {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());
            result.put("limit", command.limit);
            return result;
        }

        @Override
        public String getTypeName() {
            return "ConsumeAsMuchAsPossibleCommand";
        }

    };

    public ConsumeAsMuchAsPossibleCommand(long limit) {
        this.limit = limit;
    }

    @Override
    public CommandResult<Long> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        long toConsume = Math.min(limit, availableToConsume);
        if (toConsume <= 0) {
            return CommandResult.ZERO;
        }
        state.consume(toConsume);
        mutableEntry.set(state);
        return CommandResult.success(toConsume, LONG_HANDLE);
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    @Override
    public SerializationHandle getSerializationHandle() {
        return SERIALIZATION_HANDLE;
    }

    @Override
    public boolean equalsByContent(ConsumeAsMuchAsPossibleCommand other) {
        return limit == other.limit;
    }

    @Override
    public boolean isImmediateSyncRequired(long unsynchronizedTokens, long nanosSinceLastSync) {
        return limit == Long.MAX_VALUE;
    }

    @Override
    public long estimateTokensToConsume() {
        return limit;
    }

    @Override
    public long getConsumedTokens(Long result) {
        return result;
    }

    @Override
    public Version getRequiredVersion() {
        return v_7_0_0;
    }

    @Override
    public boolean isMerged() {
        return merged;
    }

    @Override
    public int getMergedCommandsCount() {
        return (int) limit;
    }

    @Override
    public CommandResult<?> unwrapOneResult(Long consumedTokens, int indice) {
        boolean wasConsumed = indice < consumedTokens;
        return wasConsumed ?  CommandResult.TRUE : CommandResult.FALSE;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

}
