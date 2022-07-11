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

import static io.github.bucket4j.distributed.serialization.PrimitiveSerializationHandles.LONG_HANDLE;
import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class ConsumeIgnoringRateLimitsCommand implements RemoteCommand<Long>, ComparableByContent<ConsumeIgnoringRateLimitsCommand> {

    private long tokensToConsume;

    public static final SerializationHandle<ConsumeIgnoringRateLimitsCommand> SERIALIZATION_HANDLE = new SerializationHandle<ConsumeIgnoringRateLimitsCommand>() {
        @Override
        public <S> ConsumeIgnoringRateLimitsCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long tokensToConsume = adapter.readLong(input);

            return new ConsumeIgnoringRateLimitsCommand(tokensToConsume);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ConsumeIgnoringRateLimitsCommand command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            adapter.writeLong(output, command.tokensToConsume);
        }

        @Override
        public int getTypeId() {
            return 34;
        }

        @Override
        public Class<ConsumeIgnoringRateLimitsCommand> getSerializedType() {
            return ConsumeIgnoringRateLimitsCommand.class;
        }

        @Override
        public ConsumeIgnoringRateLimitsCommand fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long tokensToConsume = readLongValue(snapshot, "tokensToConsume");
            return new ConsumeIgnoringRateLimitsCommand(tokensToConsume);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(ConsumeIgnoringRateLimitsCommand command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());
            result.put("tokensToConsume", command.tokensToConsume);
            return result;
        }

        @Override
        public String getTypeName() {
            return "ConsumeIgnoringRateLimitsCommand";
        }

    };

    public ConsumeIgnoringRateLimitsCommand(long limit) {
        this.tokensToConsume = limit;
    }

    public long getTokensToConsume() {
        return tokensToConsume;
    }

    @Override
    public CommandResult<Long> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);
        long nanosToCloseDeficit = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, false);

        if (nanosToCloseDeficit == Long.MAX_VALUE) {
            return CommandResult.success(Long.MAX_VALUE, LONG_HANDLE);
        }
        state.consume(tokensToConsume);
        mutableEntry.set(state);
        return CommandResult.success(nanosToCloseDeficit, LONG_HANDLE);
    }

    @Override
    public SerializationHandle<RemoteCommand<?>> getSerializationHandle() {
        return (SerializationHandle) SERIALIZATION_HANDLE;
    }

    @Override
    public boolean equalsByContent(ConsumeIgnoringRateLimitsCommand other) {
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
    public long getConsumedTokens(Long result) {
        return result == Long.MAX_VALUE? 0l: tokensToConsume;
    }

    @Override
    public Version getRequiredVersion() {
        return v_7_0_0;
    }

}
