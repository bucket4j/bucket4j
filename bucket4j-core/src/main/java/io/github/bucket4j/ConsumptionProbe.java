/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j;

import io.github.bucket4j.distributed.proxy.AsyncBucketProxy;

import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;
import java.io.Serializable;

/**
 * Describes both result of consumption and tokens remaining in the bucket after consumption.
 *
 * @see Bucket#tryConsumeAndReturnRemaining(long)
 * @see AsyncBucketProxy#tryConsumeAndReturnRemaining(long)
 */
public class ConsumptionProbe implements Serializable {

    private static final long serialVersionUID = 42L;

    private final boolean consumed;
    private final long remainingTokens;
    private final long nanosToWaitForRefill;
    private final long nanosToWaitForReset;

    public static SerializationHandle<ConsumptionProbe> SERIALIZATION_HANDLE = new SerializationHandle<ConsumptionProbe>() {
        @Override
        public <S> ConsumptionProbe deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            boolean consumed = adapter.readBoolean(input);
            long remainingTokens = adapter.readLong(input);
            long nanosToWaitForRefill = adapter.readLong(input);
            long nanosToWaitForReset = adapter.readLong(input);

            return new ConsumptionProbe(consumed, remainingTokens, nanosToWaitForRefill, nanosToWaitForReset);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ConsumptionProbe probe) throws IOException {
            adapter.writeBoolean(output, probe.consumed);
            adapter.writeLong(output, probe.remainingTokens);
            adapter.writeLong(output, probe.nanosToWaitForRefill);
            adapter.writeLong(output, probe.nanosToWaitForReset);
        }

        @Override
        public int getTypeId() {
            return 15;
        }

        @Override
        public Class<ConsumptionProbe> getSerializedType() {
            return ConsumptionProbe.class;
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

}
