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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.github.bucket4j.EstimationProbe;
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

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;


public class EstimateAbilityToConsumeCommand implements RemoteCommand<EstimationProbe>, ComparableByContent<EstimateAbilityToConsumeCommand> {

    private final long tokensToConsume;

    public static final SerializationHandle<EstimateAbilityToConsumeCommand> SERIALIZATION_HANDLE = new SerializationHandle<>() {
        @Override
        public <S> EstimateAbilityToConsumeCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long tokensToConsume = adapter.readLong(input);

            return new EstimateAbilityToConsumeCommand(tokensToConsume);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, EstimateAbilityToConsumeCommand command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            adapter.writeLong(output, command.tokensToConsume);
        }

        @Override
        public int getTypeId() {
            return 28;
        }

        @Override
        public Class<EstimateAbilityToConsumeCommand> getSerializedType() {
            return EstimateAbilityToConsumeCommand.class;
        }

        @Override
        public EstimateAbilityToConsumeCommand fromJsonCompatibleSnapshot(Map<String, Object> snapshot) {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long tokensToConsume = readLongValue(snapshot, "tokensToConsume");
            return new EstimateAbilityToConsumeCommand(tokensToConsume);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(EstimateAbilityToConsumeCommand command, Version backwardCompatibilityVersion, Scope scope) {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());
            result.put("tokensToConsume", command.tokensToConsume);
            return result;
        }

        @Override
        public String getTypeName() {
            return "EstimateAbilityToConsumeCommand";
        }

    };

    public EstimateAbilityToConsumeCommand(long tokensToEstimate) {
        this.tokensToConsume = tokensToEstimate;
    }

    @Override
    public CommandResult<EstimationProbe> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToConsume <= availableToConsume) {
            EstimationProbe estimationProbe = EstimationProbe.canBeConsumed(availableToConsume);
            return CommandResult.success(estimationProbe, EstimationProbe.SERIALIZATION_HANDLE);
        } else {
            long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, true);
            EstimationProbe estimationProbe = EstimationProbe.canNotBeConsumed(availableToConsume, nanosToWaitForRefill);
            return CommandResult.success(estimationProbe, EstimationProbe.SERIALIZATION_HANDLE);
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
    public boolean equalsByContent(EstimateAbilityToConsumeCommand other) {
        return tokensToConsume == other.tokensToConsume;
    }

    @Override
    public boolean isImmediateSyncRequired(long unsynchronizedTokens, long nanosSinceLastSync) {
        return false;
    }

    @Override
    public long estimateTokensToConsume() {
        return 0;
    }

    @Override
    public long getConsumedTokens(EstimationProbe result) {
        return 0;
    }

    @Override
    public Version getRequiredVersion() {
        return v_7_0_0;
    }

}
