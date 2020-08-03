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
package io.github.bucket4j;

import io.github.bucket4j.distributed.proxy.DefaultAsyncBucketProxy;

import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;

import static io.github.bucket4j.distributed.versioning.Versions.v_5_0_0;

/**
 * Describes the estimation result.
 *
 * @see Bucket#estimateAbilityToConsume(long)
 * @see DefaultAsyncBucketProxy#estimateAbilityToConsume(long)
 */
public class EstimationProbe implements ComparableByContent<EstimationProbe> {

    private final boolean canBeConsumed;
    private final long remainingTokens;
    private final long nanosToWaitForRefill;

    public static final SerializationHandle<EstimationProbe> SERIALIZATION_HANDLE = new SerializationHandle<EstimationProbe>() {
        @Override
        public <S> EstimationProbe deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_5_0_0, v_5_0_0);

            boolean canBeConsumed = adapter.readBoolean(input);
            long remainingTokens = adapter.readLong(input);
            long nanosToWaitForRefill = adapter.readLong(input);

            return new EstimationProbe(canBeConsumed, remainingTokens, nanosToWaitForRefill);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, EstimationProbe probe, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeInt(output, v_5_0_0.getNumber());

            adapter.writeBoolean(output, probe.canBeConsumed);
            adapter.writeLong(output, probe.remainingTokens);
            adapter.writeLong(output, probe.nanosToWaitForRefill);
        }

        @Override
        public int getTypeId() {
            return 12;
        }

        @Override
        public Class<EstimationProbe> getSerializedType() {
            return EstimationProbe.class;
        }

    };

    public static EstimationProbe canBeConsumed(long remainingTokens) {
        return new EstimationProbe(true, remainingTokens, 0);
    }

    public static EstimationProbe canNotBeConsumed(long remainingTokens, long nanosToWaitForRefill) {
        return new EstimationProbe(false, remainingTokens, nanosToWaitForRefill);
    }

    private EstimationProbe(boolean canBeConsumed, long remainingTokens, long nanosToWaitForRefill) {
        this.canBeConsumed = canBeConsumed;
        this.remainingTokens = Math.max(0L, remainingTokens);
        this.nanosToWaitForRefill = nanosToWaitForRefill;
    }

    /**
     * Flag describes result of consumption operation.
     *
     * @return true if requested tokens can be consumed
     */
    public boolean canBeConsumed() {
        return canBeConsumed;
    }

    /**
     * Return the tokens remaining in the bucket
     *
     * @return the tokens remaining in the bucket
     */
    public long getRemainingTokens() {
        return remainingTokens;
    }

    /**
     * Returns zero if {@link #canBeConsumed()} returns true, else time in nanos which need to wait until requested amount of tokens will be refilled
     *
     * @return Zero if {@link #canBeConsumed()} returns true, else time in nanos which need to wait until requested amount of tokens will be refilled
     */
    public long getNanosToWaitForRefill() {
        return nanosToWaitForRefill;
    }

    @Override
    public String toString() {
        return "ConsumptionResult{" +
                "isConsumed=" + canBeConsumed +
                ", remainingTokens=" + remainingTokens +
                ", nanosToWaitForRefill=" + nanosToWaitForRefill +
                '}';
    }

    @Override
    public boolean equalsByContent(EstimationProbe other) {
        return canBeConsumed == other.canBeConsumed &&
                remainingTokens == other.remainingTokens &&
                nanosToWaitForRefill == other.nanosToWaitForRefill;
    }

}
