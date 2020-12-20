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

import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.serialization.SerializationAdapter;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;

/**
 * <h3>Anatomy of bandwidth:</h3>
 * The bandwidth is key building block for bucket.
 * The bandwidth consists from {@link #capacity} and {@link Refill refill}. Where:
 * <ul>
 *     <li><b>Capacity</b> - defines the maximum count of tokens which can be hold by bucket.</li>
 *     <li><b>Refill</b> - defines the speed in which tokens are regenerated in bucket.</li>
 * </ul>
 *
 * <h3>Classic and simple bandwidth definitions:</h3>
 * The bandwidth can be initialized in the two way:
 * <ul>
 *     <li>{@link #simple(long, Duration) Simple} - most popular way, which does not require from you to fully understand the token-bucket algorithm.
 *     Use this way when you just want to specify easy limitation <tt>N</tt> tokens per <tt>M</tt> time window.
 *     See <a href="https://github.com/vladimir-bukhtoyarov/bucket4j/blob/1.3/doc-pages/basic-usage.md#example-1---limiting-the-rate-of-heavy-work">this example</a> of usage.
 *     </li>
 *     <li>{@link #classic(long, Refill)} Classic} - hard way to specify limitation,
 *     use it when you want to utilize the whole power of token-bucket. See <a href="https://github.com/vladimir-bukhtoyarov/bucket4j/blob/1.3/doc-pages/basic-usage.md#example-3---limiting-the-rate-of-access-to-rest-api">this example</a> of usage.
 *     </li>
 * </ul>
 *
 * <h3>Multiple bandwidths:</h3>
 * Most likely you will use only one bandwidth per bucket,
 * but in general it is possible to specify more than one bandwidth per bucket,
 * and bucket will handle all bandwidth in strongly atomic way.
 * Strongly atomic means that token will be consumed from all bandwidth or from nothing,
 * in other words any token can not be partially consumed.
 * <br> Example of multiple bandwidth:
 * <pre>{@code // Adds bandwidth that restricts to consume not often 1000 tokens per 1 minute and not often than 100 tokens per second
 * Bucket bucket = Bucket4j.builder().
 *      .addLimit(Bandwidth.create(1000, Duration.ofMinutes(1)));
 *      .addLimit(Bandwidth.create(100, Duration.ofSeconds(1)));
 *      .build()
 * }</pre>
 */
public class Bandwidth implements Serializable {

    private static final long serialVersionUID = 101L;

    public static final String UNDEFINED_ID = null;

    final long capacity;
    final long initialTokens;
    final long refillPeriodNanos;
    final long refillTokens;
    final boolean refillIntervally;
    final long timeOfFirstRefillMillis;
    final boolean useAdaptiveInitialTokens;
    final String id;

    private Bandwidth(long capacity, long refillPeriodNanos, long refillTokens, long initialTokens, boolean refillIntervally,
              long timeOfFirstRefillMillis, boolean useAdaptiveInitialTokens, String id) {
        this.capacity = capacity;
        this.initialTokens = initialTokens;
        this.refillPeriodNanos = refillPeriodNanos;
        this.refillTokens = refillTokens;
        this.refillIntervally = refillIntervally;
        this.timeOfFirstRefillMillis = timeOfFirstRefillMillis;
        this.useAdaptiveInitialTokens = useAdaptiveInitialTokens;
        this.id = id;
    }

    /**
     * Specifies simple limitation <tt>capacity</tt> tokens per <tt>period</tt> time window.
     *
     * @param capacity
     * @param period
     *
     * @return
     */
    public static Bandwidth simple(long capacity, Duration period) {
        Refill refill = Refill.greedy(capacity, period);
        return classic(capacity, refill);
    }

    /**
     * Specifies limitation in <a href="https://github.com/vladimir-bukhtoyarov/bucket4j/blob/1.3/doc-pages/token-bucket-brief-overview.md#token-bucket-algorithm">classic interpretation</a> of token-bucket algorithm.
     *
     * @param capacity
     * @param refill
     *
     * @return
     */
    public static Bandwidth classic(long capacity, Refill refill) {
        if (capacity <= 0) {
            throw BucketExceptions.nonPositiveCapacity(capacity);
        }
        if (refill == null) {
            throw BucketExceptions.nullBandwidthRefill();
        }
        return new Bandwidth(capacity, refill.periodNanos, refill.tokens, capacity, refill.refillIntervally,
                refill.timeOfFirstRefillMillis, refill.useAdaptiveInitialTokens, UNDEFINED_ID);
    }

    /**
     * By default new created bandwidth has amount tokens that equals its capacity.
     * This method allows to replace initial tokens.
     *
     * @param initialTokens
     *
     * @return the copy of this bandwidth with new value ofof initial tokens.
     */
    public Bandwidth withInitialTokens(long initialTokens) {
        if (initialTokens < 0) {
            throw BucketExceptions.nonPositiveInitialTokens(initialTokens);
        }
        if (isIntervallyAligned() && useAdaptiveInitialTokens) {
            throw BucketExceptions.intervallyAlignedRefillWithAdaptiveInitialTokensIncompatipleWithManualSpecifiedInitialTokens();
        }
        return new Bandwidth(capacity, refillPeriodNanos, refillTokens, initialTokens, refillIntervally,
                timeOfFirstRefillMillis, useAdaptiveInitialTokens, UNDEFINED_ID);
    }

    /**
     * By default new created bandwidth has no ID.
     * This method allows to specify unique identifier of bandwidth that can be used for bandwidth comparision during configuration replacement {@link Bucket#replaceConfiguration(BucketConfiguration)}
     *
     * @param id unique identifier of bandwidth that can be used for bandwidth comparision during configuration replacement {@link Bucket#replaceConfiguration(BucketConfiguration)}
     *
     * @return the copy of this bandwidth with new value ofof initial tokens.
     */
    public Bandwidth withId(String id) {
        return new Bandwidth(capacity, refillPeriodNanos, refillTokens, initialTokens, refillIntervally,
                timeOfFirstRefillMillis, useAdaptiveInitialTokens, id);
    }

    public boolean isIntervallyAligned() {
        return timeOfFirstRefillMillis != Refill.UNSPECIFIED_TIME_OF_FIRST_REFILL;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getInitialTokens() {
        return initialTokens;
    }

    public long getRefillPeriodNanos() {
        return refillPeriodNanos;
    }

    public long getRefillTokens() {
        return refillTokens;
    }

    public boolean isRefillIntervally() {
        return refillIntervally;
    }

    public boolean isUseAdaptiveInitialTokens() {
        return useAdaptiveInitialTokens;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public long getTimeOfFirstRefillMillis() {
        return timeOfFirstRefillMillis;
    }

    public boolean isGready() {
        return !refillIntervally;
    }

    public String getId() {
        return id;
    }

    public static final SerializationHandle<Bandwidth> SERIALIZATION_HANDLE = new SerializationHandle<Bandwidth>() {
        @Override
        public <S> Bandwidth deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            long capacity = adapter.readLong(input);
            long initialTokens = adapter.readLong(input);
            long refillPeriodNanos = adapter.readLong(input);
            long refillTokens = adapter.readLong(input);
            boolean refillIntervally = adapter.readBoolean(input);
            long timeOfFirstRefillMillis = adapter.readLong(input);
            boolean useAdaptiveInitialTokens = adapter.readBoolean(input);
            boolean hasId = adapter.readBoolean(input);
            String id = hasId? adapter.readString(input) : UNDEFINED_ID;

            return new Bandwidth(capacity, refillPeriodNanos, refillTokens, initialTokens, refillIntervally,
                    timeOfFirstRefillMillis, useAdaptiveInitialTokens, id);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, Bandwidth bandwidth) throws IOException {
            adapter.writeLong(output, bandwidth.capacity);
            adapter.writeLong(output, bandwidth.initialTokens);
            adapter.writeLong(output, bandwidth.refillPeriodNanos);
            adapter.writeLong(output, bandwidth.refillTokens);
            adapter.writeBoolean(output, bandwidth.refillIntervally);
            adapter.writeLong(output, bandwidth.timeOfFirstRefillMillis);
            adapter.writeBoolean(output, bandwidth.useAdaptiveInitialTokens);
            adapter.writeBoolean(output, bandwidth.id != null);
            if (bandwidth.hasId()) {
                adapter.writeString(output, bandwidth.id);
            }
        }

        @Override
        public int getTypeId() {
            return 1;
        }

        @Override
        public Class<Bandwidth> getSerializedType() {
            return Bandwidth.class;
        }

    };

    public boolean hasId() {
        return id != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Bandwidth bandwidth = (Bandwidth) o;

        if (capacity != bandwidth.capacity) { return false; }
        if (initialTokens != bandwidth.initialTokens) { return false; }
        if (refillPeriodNanos != bandwidth.refillPeriodNanos) { return false; }
        if (refillTokens != bandwidth.refillTokens) { return false; }
        if (refillIntervally != bandwidth.refillIntervally) { return false; }
        if (timeOfFirstRefillMillis != bandwidth.timeOfFirstRefillMillis) { return false; }
        return useAdaptiveInitialTokens == bandwidth.useAdaptiveInitialTokens;
    }

    @Override
    public int hashCode() {
        int result = (int) (capacity ^ (capacity >>> 32));
        result = 31 * result + (int) (initialTokens ^ (initialTokens >>> 32));
        result = 31 * result + (int) (refillPeriodNanos ^ (refillPeriodNanos >>> 32));
        result = 31 * result + (int) (refillTokens ^ (refillTokens >>> 32));
        result = 31 * result + (refillIntervally ? 1 : 0);
        result = 31 * result + (int) (timeOfFirstRefillMillis ^ (timeOfFirstRefillMillis >>> 32));
        result = 31 * result + (useAdaptiveInitialTokens ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Bandwidth{");
        sb.append("capacity=").append(capacity);
        sb.append(", initialTokens=").append(initialTokens);
        sb.append(", refillPeriodNanos=").append(refillPeriodNanos);
        sb.append(", refillTokens=").append(refillTokens);
        sb.append(", refillIntervally=").append(refillIntervally);
        sb.append(", timeOfFirstRefillMillis=").append(timeOfFirstRefillMillis);
        sb.append(", useAdaptiveInitialTokens=").append(useAdaptiveInitialTokens);
        sb.append('}');
        return sb.toString();
    }

}
