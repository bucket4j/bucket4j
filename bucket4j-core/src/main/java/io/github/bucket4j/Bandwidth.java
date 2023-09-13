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

import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.local.LocalBucketBuilder;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

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
 *     </li>
 *     <li>{@link #classic(long, Refill)} Classic} - hard way to specify limitation,
 *     use it when you want to utilize the whole power of token-bucket.
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
 * Bucket bucket = Bucket.builder().
 *      .addLimit(Bandwidth.create(1000, Duration.ofMinutes(1)));
 *      .addLimit(Bandwidth.create(100, Duration.ofSeconds(1)));
 *      .build()
 * }</pre>
 */
public class Bandwidth implements ComparableByContent<Bandwidth> {

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
     * Creates a builder for {@link Bandwidth}
     *
     * @return a builder for {@link Bandwidth}
     */
    public static BandwidthBuilderCapacityStage builder() {
        return new BandwidthBuilder();
    }

    /**
     * This method is deprecated, you should use {@link Bandwidth#builder()}
     *
     * @deprecated
     */
    @Deprecated
    public static Bandwidth simple(long capacity, Duration period) {
        Refill refill = Refill.greedy(capacity, period);
        return classic(capacity, refill);
    }

    /**
     * This method is deprecated, you should use {@link Bandwidth#builder()}
     *
     * @deprecated
     */
    @Deprecated
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
     * This method is deprecated, you should use {@link Bandwidth#builder()}
     *
     * @deprecated
     */
    @Deprecated
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
     * This method is deprecated, you should use {@link Bandwidth#builder()}
     *
     * @deprecated
     */
    @Deprecated
    public Bandwidth withId(String id) {
        return new Bandwidth(capacity, refillPeriodNanos, refillTokens, initialTokens, refillIntervally,
                timeOfFirstRefillMillis, useAdaptiveInitialTokens, id);
    }

    public boolean isIntervallyAligned() {
        return timeOfFirstRefillMillis != Refill.UNSPECIFIED_TIME_OF_FIRST_REFILL;
    }

    public long getTimeOfFirstRefillMillis() {
        return timeOfFirstRefillMillis;
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

    public boolean isUseAdaptiveInitialTokens() {
        return useAdaptiveInitialTokens;
    }

    public boolean isRefillIntervally() {
        return refillIntervally;
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
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

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
        public <O> void serialize(SerializationAdapter<O> adapter, O output, Bandwidth bandwidth, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

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

        @Override
        public Bandwidth fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long capacity = readLongValue(snapshot, "capacity");
            long initialTokens = readLongValue(snapshot, "initialTokens");
            long refillPeriodNanos = readLongValue(snapshot, "refillPeriodNanos");
            long refillTokens = readLongValue(snapshot, "refillTokens");
            boolean refillIntervally = (boolean) snapshot.get("refillIntervally");
            long timeOfFirstRefillMillis = readLongValue(snapshot, "timeOfFirstRefillMillis");
            boolean useAdaptiveInitialTokens = (boolean) snapshot.get("useAdaptiveInitialTokens");
            String id = (String) snapshot.get("id");

            return new Bandwidth(capacity, refillPeriodNanos, refillTokens, initialTokens, refillIntervally,
                    timeOfFirstRefillMillis, useAdaptiveInitialTokens, id);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(Bandwidth bandwidth, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());

            result.put("capacity", bandwidth.capacity);
            result.put("initialTokens", bandwidth.initialTokens);
            result.put("refillPeriodNanos", bandwidth.refillPeriodNanos);
            result.put("refillTokens", bandwidth.refillTokens);
            result.put("refillIntervally", bandwidth.refillIntervally);
            result.put("timeOfFirstRefillMillis", bandwidth.timeOfFirstRefillMillis);
            result.put("useAdaptiveInitialTokens", bandwidth.useAdaptiveInitialTokens);
            if (bandwidth.id != null) {
                result.put("id", bandwidth.id);
            }
            return result;
        }

        @Override
        public String getTypeName() {
            return "Bandwidth";
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

    @Override
    public boolean equalsByContent(Bandwidth other) {
        return capacity == other.capacity &&
                initialTokens == other.initialTokens &&
                refillPeriodNanos == other.refillPeriodNanos &&
                refillTokens == other.refillTokens &&
                refillIntervally == other.refillIntervally &&
                timeOfFirstRefillMillis == other.timeOfFirstRefillMillis &&
                useAdaptiveInitialTokens == other.useAdaptiveInitialTokens;
    }

    public interface BandwidthBuilderCapacityStage {

        BandwidthBuilderRefillStage capacity(long tokens);

    }

    public interface BandwidthBuilderRefillStage {
        /**
         * Creates the {@link Refill} that does refill of tokens in greedy manner,
         * it will try to add the tokens to bucket as soon as possible.
         * For example refill "10 tokens per 1 second" will add 1 token per each 100 millisecond,
         * in other words refill will not wait 1 second to regenerate a bunch of 10 tokens.
         *
         * <p>
         * The three refills bellow do refill of tokens with same speed:
         * <pre>
         *      <code>Refill.greedy(600, Duration.ofMinutes(1));</code>
         *      <code>Refill.greedy(10, Duration.ofSeconds(1));</code>
         *      <code>Refill.greedy(1, Duration.ofMillis(100));</code>
         * </pre>
         *
         * <p>
         * If greediness is undesired then you can specify the fixed interval refill via {@link #intervally(long, Duration)}
         *
         * @param tokens amount of tokens
         * @param period the period within {@code tokens} will be fully regenerated
         *
         * @return the {@link Refill} that does refill of tokens in of manner
         */
        BandwidthBuilderBuildStage refillGreedy(long tokens, Duration period);

        /**
         * Creates the {@link Refill} that does refill of tokens in intervally manner.
         * "Intervally" in opposite to "greedy"  will wait until whole {@code period} will be elapsed before regenerate {@code tokens}
         *
         * @param tokens amount of tokens
         * @param period the period within {@code tokens} will be fully regenerated
         *
         * @return the {@link Refill} that does refill of tokens in intervally manner
         */
        BandwidthBuilderBuildStage refillIntervally(long tokens, Duration period);

        /**
         * Creates the {@link Refill} that does refill of tokens in intervally manner.
         * "Intervally" in opposite to "greedy"  will wait until whole {@code period} will be elapsed before regenerate {@code tokens}.
         * <br>
         * In additional to {@link #intervally(long, Duration)} it is possible to specify the time when first refill should happen via {@code timeOfFirstRefill}.
         * This option can be used to configure clear interval boundary i.e. start of second, minute, hour, day.
         *
         * <p>
         *     <strong>Special notes about useAdaptiveInitialTokens:</strong>
         * <br>If {@code useAdaptiveInitialTokens == true} and timeOfFirstRefill is a moment in the future, then initial amount of tokens in the bandwidth will be calculated by following formula:
         * <br><pre>{@code
         *     Math.min(capacity, Math.max(0, bandwidthCapacity - refillTokens) + (timeOfFirstRefillMillis - nowMillis)/refillPeriod * refillTokens)
         * }</pre>
         * <br>Bellow the list of examples of how does this formula can be applied:
         * <pre>
         * {@code
         *         // imagine that wall clock is 16:20, the first refill will happen at 17:00
         *         // first refill will happen in the beginning of next hour
         *         Instant firstRefillTime = ZonedDateTime.now()
         *                 .truncatedTo(ChronoUnit.HOURS)
         *                 .plus(1, ChronoUnit.HOURS)
         *                 .toInstant();
         *
         *        // initial tokens 266 will be
         *        Bandwidth.classic(400, Refill.intervallyAligned(400, Duration.ofHours(1), firstRefillTime, true));
         *        // calculated by formula min(400, max(0, 400 - 400) +  40/60*400) = min(400, 0 + 266) = 266
         *
         *        // initial tokens will be 300
         *        Bandwidth.classic(400, Refill.intervallyAligned(300, Duration.ofHours(1), firstRefillTime, true));
         *        // calculated by formula min(400, max(0, 400 - 300) +  40/60*300) = min(400, 100 + 200) = 300
         *
         *        // initial tokens will be 333
         *        Bandwidth.classic(400, Refill.intervallyAligned(200, Duration.ofHours(1), firstRefillTime, true));
         *        // calculated by formula min(400, max(0, 400 - 200) +  40/60*200) = min(400, 200 + 133) = 333
         *
         *        // initial tokens will be 366
         *        Bandwidth.classic(400, Refill.intervallyAligned(100, Duration.ofHours(1), firstRefillTime, true));
         *        // calculated by formula min(400, max(0, 400 - 100) +  40/60*100) = min(400, 300 + 66) = 366
         * }</pre>
         *
         *
         *     <strong>Restrictions:</strong>
         *     <ul>
         *         <li>
         *             If {@code useAdaptiveInitialTokens} is {@code true} then any attempt to explicitly specify initial amount of tokens via {@link Bandwidth#withInitialTokens(long)}
         *             will fail with exception, because it is impossible at the same time to specify tokens in explicitly and adaptively manners.
         *         </li>
         *         <li>
         *             It is impossible to use this method together with nanoTime based clock {@link LocalBucketBuilder#withNanosecondPrecision()}, because we need in {@link System#currentTimeMillis()} based clock
         *             in order to properly measure the distance from {@code timeOfFirstRefill}
         *         </li>
         *     </ul>
         *
         *
         * @param tokens amount of tokens
         * @param period the period within {@code tokens} will be fully regenerated
         * @param timeOfFirstRefill the time of first refill, typically it should be a moment in the future
         *
         * @return the {@link Refill} that does refill of tokens in intervally manner
         */
        BandwidthBuilderBuildStage refillIntervallyAligned(long tokens, Duration period, Instant timeOfFirstRefill);

        /**
         * Creates the {@link Refill} that does refill of tokens in intervally manner.
         * "Intervally" in opposite to "greedy"  will wait until whole {@code period} will be elapsed before regenerate {@code tokens}.
         * <br>
         * In additional to {@link #intervally(long, Duration)} it is possible to specify the time when first refill should happen via {@code timeOfFirstRefill}.
         * This option can be used to configure clear interval boundary i.e. start of second, minute, hour, day.
         *
         * <p>
         *     <strong>Special notes about useAdaptiveInitialTokens:</strong>
         * <br>If {@code useAdaptiveInitialTokens == true} and timeOfFirstRefill is a moment in the future, then initial amount of tokens in the bandwidth will be calculated by following formula:
         * <br><pre>{@code
         *     Math.min(capacity, Math.max(0, bandwidthCapacity - refillTokens) + (timeOfFirstRefillMillis - nowMillis)/refillPeriod * refillTokens)
         * }</pre>
         * <br>Bellow the list of examples of how does this formula can be applied:
         * <pre>
         * {@code
         *         // imagine that wall clock is 16:20, the first refill will happen at 17:00
         *         // first refill will happen in the beginning of next hour
         *         Instant firstRefillTime = ZonedDateTime.now()
         *                 .truncatedTo(ChronoUnit.HOURS)
         *                 .plus(1, ChronoUnit.HOURS)
         *                 .toInstant();
         *
         *        // initial tokens 266 will be
         *        Bandwidth.classic(400, Refill.intervallyAligned(400, Duration.ofHours(1), firstRefillTime, true));
         *        // calculated by formula min(400, max(0, 400 - 400) +  40/60*400) = min(400, 0 + 266) = 266
         *
         *        // initial tokens will be 300
         *        Bandwidth.classic(400, Refill.intervallyAligned(300, Duration.ofHours(1), firstRefillTime, true));
         *        // calculated by formula min(400, max(0, 400 - 300) +  40/60*300) = min(400, 100 + 200) = 300
         *
         *        // initial tokens will be 333
         *        Bandwidth.classic(400, Refill.intervallyAligned(200, Duration.ofHours(1), firstRefillTime, true));
         *        // calculated by formula min(400, max(0, 400 - 200) +  40/60*200) = min(400, 200 + 133) = 333
         *
         *        // initial tokens will be 366
         *        Bandwidth.classic(400, Refill.intervallyAligned(100, Duration.ofHours(1), firstRefillTime, true));
         *        // calculated by formula min(400, max(0, 400 - 100) +  40/60*100) = min(400, 300 + 66) = 366
         * }</pre>
         *
         *
         *  <strong>Restrictions:</strong>
         *  <ul>
         *     <li>
         *        If {@code useAdaptiveInitialTokens} is {@code true} then any attempt to explicitly specify initial amount of tokens via {@link Bandwidth#withInitialTokens(long)}
         *        will fail with exception, because it is impossible at the same time to specify tokens in explicitly and adaptively manners.
         *     </li>
         *     <li>
         *        It is impossible to use this method together with nanoTime based clock {@link LocalBucketBuilder#withNanosecondPrecision()}, because we need in {@link System#currentTimeMillis()} based clock
         *        in order to properly measure the distance from {@code timeOfFirstRefill}
         *     </li>
         *  </ul>
         *
         *
         * @param tokens amount of tokens
         * @param period the period within {@code tokens} will be fully regenerated
         * @param timeOfFirstRefill the time of first refill, typically it should be a moment in the future
         *
         * @return the {@link Refill} that does refill of tokens in intervally manner
         */
        BandwidthBuilderBuildStage refillIntervallyAlignedWithAdaptiveInitialTokens(long tokens, Duration period, Instant timeOfFirstRefill);

    }

    public interface BandwidthBuilderBuildStage {

        /**
         * Creates the new instance of {@link Bandwidth}
         *
         * @return new instance of {@link Bandwidth}
         */
        Bandwidth build();

        /**
         * By default, new created bandwidth has no ID.
         * This method allows to specify unique identifier of bandwidth that can be used for bandwidth comparision during configuration replacement {@link Bucket#replaceConfiguration(BucketConfiguration, TokensInheritanceStrategy)}
         *
         * @param id unique identifier of bandwidth that can be used for bandwidth comparision during configuration replacement {@link Bucket#replaceConfiguration(BucketConfiguration, TokensInheritanceStrategy)}
         *
         * @return the copy of this bandwidth with new value ofof initial tokens.
         */
        BandwidthBuilderBuildStage id(String id);

        /**
         * By default, new created {@link Bandwidth} has amount tokens that equals its capacity.
         * This method allows to replace initial tokens.
         *
         * @param initialTokens
         *
         * @return the copy of this bandwidth with new value ofof initial tokens.
         */
        BandwidthBuilderBuildStage initialTokens(long initialTokens);

    }

}
