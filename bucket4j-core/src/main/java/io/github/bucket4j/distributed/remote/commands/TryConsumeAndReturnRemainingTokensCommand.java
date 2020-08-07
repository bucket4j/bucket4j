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

import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;

import java.io.IOException;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import static io.github.bucket4j.distributed.versioning.Versions.v_5_0_0;


public class TryConsumeAndReturnRemainingTokensCommand implements RemoteCommand<ConsumptionProbe>, ComparableByContent<TryConsumeAndReturnRemainingTokensCommand> {

    private long tokensToConsume;

    public static final SerializationHandle<TryConsumeAndReturnRemainingTokensCommand> SERIALIZATION_HANDLE = new SerializationHandle<TryConsumeAndReturnRemainingTokensCommand>() {
        @Override
        public <S> TryConsumeAndReturnRemainingTokensCommand deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_5_0_0, v_5_0_0);

            long tokensToConsume = adapter.readLong(input);

            return new TryConsumeAndReturnRemainingTokensCommand(tokensToConsume);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, TryConsumeAndReturnRemainingTokensCommand command, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeInt(output, v_5_0_0.getNumber());

            adapter.writeLong(output, command.tokensToConsume);
        }

        @Override
        public int getTypeId() {
            return 30;
        }

        @Override
        public Class<TryConsumeAndReturnRemainingTokensCommand> getSerializedType() {
            return TryConsumeAndReturnRemainingTokensCommand.class;
        }

    };

    public TryConsumeAndReturnRemainingTokensCommand(long tokensToConsume) {
        this.tokensToConsume = tokensToConsume;
    }

    @Override
    public CommandResult<ConsumptionProbe> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToConsume <= availableToConsume) {
            state.consume(tokensToConsume);
            mutableEntry.set(state);
            long nanosToWaitForReset = state.calculateFullRefillingTime(currentTimeNanos);
            long remainingTokens = availableToConsume - tokensToConsume;
            ConsumptionProbe probe = ConsumptionProbe.consumed(remainingTokens, nanosToWaitForReset);
            return CommandResult.success(probe, ConsumptionProbe.SERIALIZATION_HANDLE);
        } else {
            long nanosToWaitForReset = state.calculateFullRefillingTime(currentTimeNanos);
            long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);
            ConsumptionProbe probe = ConsumptionProbe.rejected(availableToConsume, nanosToWaitForRefill, nanosToWaitForReset);
            return CommandResult.success(probe, ConsumptionProbe.SERIALIZATION_HANDLE);
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
    public boolean equalsByContent(TryConsumeAndReturnRemainingTokensCommand other) {
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
    public long getConsumedTokens(ConsumptionProbe result) {
        return result.isConsumed() ? tokensToConsume : 0L;
    }

}
