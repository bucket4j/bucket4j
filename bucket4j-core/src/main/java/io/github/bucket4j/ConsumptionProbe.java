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

/**
 * Describes tokens consumed, tokens remaining, time required for token regeneration to occur, and
 * the current bucket configuration after consumption.
 *
 * @see Bucket#tryConsumeAndReturnRemaining(long)
 * @see DefaultAsyncBucketProxy#tryConsumeAndReturnRemaining(long)
 */
public class ConsumptionProbe implements ComparableByContent<ConsumptionProbe> {

    private final boolean consumed;
    private final long remainingTokens;
    private final long nanosToWaitForRefill;
    private final long nanosToWaitForReset;

    public static final SerializationHandle<ConsumptionProbe> SERIALIZATION_HANDLE = new SerializationHandle<ConsumptionProbe>() {
        @Override
        public <S> ConsumptionProbe deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            boolean consumed = adapter.readBoolean(input);
            long remainingTokens = adapter.readLong(input);
            long nanosToWaitForRefill = adapter.readLong(input);
            long nanosToWaitForReset = adapter.readLong(input);

            return new ConsumptionProbe(consumed, remainingTokens, nanosToWaitForRefill, nanosToWaitForReset);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ConsumptionProbe probe, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            adapter.writeBoolean(output, probe.consumed);
            adapter.writeLong(output, probe.remainingTokens);
            adapter.writeLong(output, probe.nanosToWaitForRefill);
            adapter.writeLong(output, probe.nanosToWaitForReset);
        }

        @Override
        public int getTypeId() {
            return 11;
        }

        @Override
        public Class<ConsumptionProbe> getSerializedType() {
            return ConsumptionProbe.class;
        }

        @Override
        public ConsumptionProbe fromJsonCompatibleSnapshot(Map<String, Object> snapshot) {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            boolean consumed = (boolean) snapshot.get("consumed");
            long remainingTokens = readLongValue(snapshot, "remainingTokens");
            long nanosToWaitForRefill = readLongValue(snapshot, "nanosToWaitForRefill");
            long nanosToWaitForReset = readLongValue(snapshot, "nanosToWaitForReset");
            return new ConsumptionProbe(consumed, remainingTokens, nanosToWaitForRefill, nanosToWaitForReset);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(ConsumptionProbe probe, Version backwardCompatibilityVersion, Scope scope) {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());
            result.put("consumed", probe.consumed);
            result.put("remainingTokens", probe.remainingTokens);
            result.put("nanosToWaitForRefill", probe.nanosToWaitForRefill);
            result.put("nanosToWaitForReset", probe.nanosToWaitForReset);
            return result;
        }

        @Override
        public String getTypeName() {
            return "ConsumptionProbe";
        }

    };

    public static ConsumptionProbe consumed(long remainingTokens, long nanosToWaitForReset) {
        return new ConsumptionProbe(true, remainingTokens, 0, nanosToWaitForReset);
    }

    public static ConsumptionProbe rejected(long remainingTokens, long nanosToWaitForRefill, long nanosToWaitForReset) {
        return new ConsumptionProbe(false, remainingTokens, nanosToWaitForRefill, nanosToWaitForReset);
    }

    private ConsumptionProbe(boolean consumed, long remainingTokens, long nanosToWaitForRefill, long nanosToWaitForReset) {
        this.consumed = consumed;
        this.remainingTokens = Math.max(0L, remainingTokens);
        this.nanosToWaitForRefill = nanosToWaitForRefill;
        this.nanosToWaitForReset = nanosToWaitForReset;
    }

    /**
     * Flag describes result of consumption operation.
     *
     * @return true if tokens was consumed
     */
    public boolean isConsumed() {
        return consumed;
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
     * Returns zero if {@link #isConsumed()} returns true, else time in nanos which need to wait until requested amount of tokens will be refilled
     *
     * @return Zero if {@link #isConsumed()} returns true, else time in nanos which need to wait until requested amount of tokens will be refilled
     */
    public long getNanosToWaitForRefill() {
        return nanosToWaitForRefill;
    }

    /**
     * Time in nanos which need to wait until bucket will be fully refilled to its maximum
     *
     * @return time in nanos which need to wait until bucket will be fully refilled to its maximum
     */
    public long getNanosToWaitForReset() {
        return nanosToWaitForReset;
    }

    @Override
    public String toString() {
        return "ConsumptionProbe{" +
                "consumed=" + consumed +
                ", remainingTokens=" + remainingTokens +
                ", nanosToWaitForRefill=" + nanosToWaitForRefill +
                ", nanosToWaitForReset=" + nanosToWaitForReset +
                '}';
    }

    @Override
    public boolean equalsByContent(ConsumptionProbe other) {
        return consumed == other.consumed &&
                remainingTokens == other.remainingTokens &&
                nanosToWaitForRefill == other.nanosToWaitForRefill &&
                nanosToWaitForReset == other.nanosToWaitForReset;
    }

}
