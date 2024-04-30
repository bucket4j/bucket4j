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

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class TryConsumeCommand implements RemoteCommand<Boolean>, ComparableByContent<TryConsumeCommand> {

    public static final TryConsumeCommand TRY_CONSUME_ONE = new TryConsumeCommand(1);

    private final long tokensToConsume;

    public static final SerializationHandle<TryConsumeCommand> SERIALIZATION_HANDLE = new SerializationHandle<>() {
        @Override
        public <S> TryConsumeCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long tokensToConsume = adapter.readLong(input);
            return TryConsumeCommand.create(tokensToConsume);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, TryConsumeCommand command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            adapter.writeLong(output, command.tokensToConsume);
        }

        @Override
        public int getTypeId() {
            return 29;
        }

        @Override
        public Class<TryConsumeCommand> getSerializedType() {
            return TryConsumeCommand.class;
        }

        @Override
        public TryConsumeCommand fromJsonCompatibleSnapshot(Map<String, Object> snapshot) {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long tokensToConsume = readLongValue(snapshot, "tokensToConsume");
            return TryConsumeCommand.create(tokensToConsume);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(TryConsumeCommand command, Version backwardCompatibilityVersion, Scope scope) {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());
            result.put("tokensToConsume", command.tokensToConsume);
            return result;
        }

        @Override
        public String getTypeName() {
            return "TryConsumeCommand";
        }

    };

    private TryConsumeCommand(long tokensToConsume) {
        this.tokensToConsume = tokensToConsume;
    }

    @Override
    public RemoteCommand<?> toMergedCommand() {
        ConsumeAsMuchAsPossibleCommand merged = new ConsumeAsMuchAsPossibleCommand(1);
        merged.setMerged(true);
        return merged;
    }

    @Override
    public boolean canBeMerged(RemoteCommand<?> another) {
        return this == TRY_CONSUME_ONE && another == TRY_CONSUME_ONE;
    }

    @Override
    public void mergeInto(RemoteCommand<?> mergedCommand) {
        ConsumeAsMuchAsPossibleCommand mergedConsume = (ConsumeAsMuchAsPossibleCommand) mergedCommand;
        mergedConsume.setLimit(mergedConsume.getLimit() + 1);
    }

    public static TryConsumeCommand create(long tokensToConsume) {
        if (tokensToConsume == 1) {
            return TRY_CONSUME_ONE;
        } else {
            return new TryConsumeCommand(tokensToConsume);
        }
    }

    @Override
    public CommandResult<Boolean> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToConsume <= availableToConsume) {
            state.consume(tokensToConsume);
            mutableEntry.set(state);
            return CommandResult.TRUE;
        } else {
            return CommandResult.FALSE;
        }
    }

    public long getTokensToConsume() {
        return tokensToConsume;
    }

    @Override
    public SerializationHandle getSerializationHandle() {
        return SERIALIZATION_HANDLE;
    }

    @Override
    public boolean equalsByContent(TryConsumeCommand other) {
        return tokensToConsume == other.tokensToConsume;
    }

    @Override
    public boolean isImmediateSyncRequired(long unsynchronizedTokens, long nanosSinceLastSync) {
        return false;
    }

    @Override
    public long estimateTokensToConsume() {
        return tokensToConsume;
    }

    @Override
    public long getConsumedTokens(Boolean result) {
        return result ? tokensToConsume : 0;
    }

    @Override
    public Version getRequiredVersion() {
        return v_7_0_0;
    }

}
