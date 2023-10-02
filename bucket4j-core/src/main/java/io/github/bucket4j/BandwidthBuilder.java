/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2023 Vladimir Bukhtoyarov
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

import java.time.Duration;
import java.time.Instant;

import io.github.bucket4j.local.LocalBucketBuilder;

import static io.github.bucket4j.Bandwidth.UNDEFINED_ID;
import static io.github.bucket4j.Bandwidth.UNSPECIFIED_TIME_OF_FIRST_REFILL;

/**
 * Provides API for bandwidth builder
 *
 * @author Vladimir Bukhtoyarov
 */
public class BandwidthBuilder {

    /**
     * Capacity configuration building stage
     */
    public interface BandwidthBuilderCapacityStage {

        /**
         * Specifying capacity in terms of <a href="https://en.wikipedia.org/wiki/Token_bucket">Token-Bucket</a>
         *
         * @param tokens capacity in terms of Token-Bucket
         *
         * @return next stage that responsible for configuration of refilling speed
         */
        BandwidthBuilderRefillStage capacity(long tokens);

    }

    /**
     * Stage is responsible for configuration of refilling speed
     */
    public interface BandwidthBuilderRefillStage {
        /**
         * Configures refill that does refill of tokens in greedy manner,
         * it will try to add the tokens to bucket as soon as possible.
         * For example refill "10 tokens per 1 second" will add 1 token per each 100 millisecond,
         * in other words refill will not wait 1 second to regenerate a bunch of 10 tokens.
         *
         * <p>
         * The three refills bellow do refill of tokens with same speed:
         * <pre>
         *      <code>limit -> refillGreedy(600, Duration.ofMinutes(1))</code>
         *      <code>limit -> refillGreedy(10, Duration.ofSeconds(1))</code>
         *      <code>limit -> refillGreedy(1, Duration.ofMillis(100))</code>
         * </pre>
         *
         * <p>
         * If greediness is undesired then you can specify the fixed interval refill via {@link #refillIntervally(long, Duration)}
         *
         * @param tokens amount of tokens
         * @param period the period within {@code tokens} will be fully regenerated
         *
         * @return the final build stage
         */
        BandwidthBuilderBuildStage refillGreedy(long tokens, Duration period);

        /**
         * Configures refill  that does refill of tokens in intervally manner.
         * "Intervally" in opposite to "greedy"  will wait until whole {@code period} will be elapsed before regenerate {@code tokens}
         *
         * @param tokens amount of tokens
         * @param period the period within {@code tokens} will be fully regenerated
         *
         * @return the final build stage
         */
        BandwidthBuilderBuildStage refillIntervally(long tokens, Duration period);

        /**
         * Configures refill that does refill of tokens in intervally manner.
         * "Intervally" in opposite to "greedy"  will wait until whole {@code period} will be elapsed before regenerate {@code tokens}.
         * <br>
         * In additional to {@link #refillIntervally(long, Duration)} this method allows to specify the time when first refill should happen via {@code timeOfFirstRefill}.
         * This option can be used to configure clear interval boundary i.e. start of second, minute, hour, day.
         *  <pre>
         * {@code
         *         // imagine that wall clock is 16:20, the first refill will happen at 17:00
         *         // first refill will happen in the beginning of next hour
         *         Instant firstRefillTime = ZonedDateTime.now()
         *                 .truncatedTo(ChronoUnit.HOURS)
         *                 .plus(1, ChronoUnit.HOURS)
         *                 .toInstant();
         *
         *        Bandwidth.builder(limit ->
         *             limit.capacity(600)
         *             .refillIntervallyAligned(400, Duration.ofHours(1), firstRefillTime)
         *        )
         *  }</pre>
         *
         *
         * @param tokens amount of tokens
         * @param period the period within {@code tokens} will be fully regenerated
         * @param timeOfFirstRefill the time of first refill, typically it should be a moment in the future
         *
         * @return the final build stage
         */
        BandwidthBuilderBuildStage refillIntervallyAligned(long tokens, Duration period, Instant timeOfFirstRefill);

        /**
         * Configures refill that does refill of tokens in intervally manner.
         * "Intervally" in opposite to "greedy"  will wait until whole {@code period} will be elapsed before regenerate {@code tokens}.
         * <br>
         * In additional to {@link #refillIntervally(long, Duration)} it is possible to specify the time when first refill should happen via {@code timeOfFirstRefill}.
         * This option can be used to configure clear interval boundary i.e. start of second, minute, hour, day.
         * Also for refill type, initial amount of tokens in the bandwidth will be calculated by following formula:
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
         *        // initial tokens will be 266
         *        Bandwidth.builder(limit ->
         *             limit.capacity(400)
         *             .refillIntervallyAlignedWithAdaptiveInitialTokens(400, Duration.ofHours(1), firstRefillTime)
         *        )
         *        // calculated by formula min(400, max(0, 400 - 400) +  40/60*400) = min(400, 0 + 266) = 266
         *
         *        // initial tokens will be 300
         *        Bandwidth.builder(limit ->
         *              limit.capacity(400)
         *              .refillIntervallyAlignedWithAdaptiveInitialTokens(300, Duration.ofHours(1), firstRefillTime)
         *        )
         *        // calculated by formula min(400, max(0, 400 - 300) +  40/60*300) = min(400, 100 + 200) = 300
         *
         *        // initial tokens will be 333
         *        Bandwidth.builder(limit ->
         *               limit.capacity(400)
         *               .refillIntervallyAlignedWithAdaptiveInitialTokens(200, Duration.ofHours(1), firstRefillTime)
         *        )
         *        // calculated by formula min(400, max(0, 400 - 200) +  40/60*200) = min(400, 200 + 133) = 333
         *
         *        // initial tokens will be 366
         *        Bandwidth.builder(limit ->
         *                limit.capacity(400)
         *                .refillIntervallyAlignedWithAdaptiveInitialTokens(100, Duration.ofHours(1), firstRefillTime)
         *        )
         *        // calculated by formula min(400, max(0, 400 - 100) +  40/60*100) = min(400, 300 + 66) = 366
         * }</pre>
         *
         *
         *  <strong>Restrictions:</strong>
         *  <ul>
         *     <li>
         *        If {@code useAdaptiveInitialTokens} is {@code true} then any attempt to explicitly specify initial amount of tokens via {@link BandwidthBuilderBuildStage#initialTokens(long)}
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
         * @return the final build stage
         */
        BandwidthBuilderBuildStage refillIntervallyAlignedWithAdaptiveInitialTokens(long tokens, Duration period, Instant timeOfFirstRefill);

    }

    /**
     * The final build stage with ability to configure optional parameters of bandwidth, like id or initial tokens
     */
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

    public static BandwidthBuilderCapacityStage builder() {
        return new BandwidthBuilderImpl();
    }

    private final static class BandwidthBuilderImpl implements BandwidthBuilderCapacityStage, BandwidthBuilderRefillStage, BandwidthBuilderBuildStage {

        private long capacity;
        private long refillPeriodNanos;
        private long refillTokens;
        private long initialTokens;
        private boolean refillIntervally;
        private long timeOfFirstRefillMillis = UNSPECIFIED_TIME_OF_FIRST_REFILL;
        private boolean useAdaptiveInitialTokens;
        private String id = UNDEFINED_ID;

        @Override
        public BandwidthBuilderRefillStage capacity(long tokens) {
            if (tokens <= 0) {
                throw BucketExceptions.nonPositiveCapacity(tokens);
            }
            this.capacity = tokens;
            this.initialTokens = tokens;
            return this;
        }

        @Override
        public BandwidthBuilderBuildStage refillGreedy(long tokens, Duration period) {
            setRefill(tokens, period, false);
            return this;
        }

        @Override
        public BandwidthBuilderBuildStage refillIntervally(long tokens, Duration period) {
            setRefill(tokens, period, true);
            return this;
        }

        @Override
        public BandwidthBuilderBuildStage refillIntervallyAligned(long tokens, Duration period, Instant timeOfFirstRefill) {
            long timeOfFirstRefillMillis = timeOfFirstRefill.toEpochMilli();
            if (timeOfFirstRefillMillis < 0) {
                throw BucketExceptions.nonPositiveTimeOfFirstRefill(timeOfFirstRefill);
            }
            setRefill(tokens, period, true);
            this.timeOfFirstRefillMillis = timeOfFirstRefillMillis;
            return this;
        }

        @Override
        public BandwidthBuilderBuildStage refillIntervallyAlignedWithAdaptiveInitialTokens(long tokens, Duration period, Instant timeOfFirstRefill) {
            long timeOfFirstRefillMillis = timeOfFirstRefill.toEpochMilli();
            if (timeOfFirstRefillMillis < 0) {
                throw BucketExceptions.nonPositiveTimeOfFirstRefill(timeOfFirstRefill);
            }
            setRefill(tokens, period, true);
            this.timeOfFirstRefillMillis = timeOfFirstRefillMillis;
            this.useAdaptiveInitialTokens = true;
            return this;
        }

        @Override
        public BandwidthBuilderBuildStage id(String id) {
            this.id = id;
            return this;
        }

        @Override
        public BandwidthBuilderBuildStage initialTokens(long initialTokens) {
            if (initialTokens < 0) {
                throw BucketExceptions.nonPositiveInitialTokens(initialTokens);
            }
            if (timeOfFirstRefillMillis != UNSPECIFIED_TIME_OF_FIRST_REFILL && useAdaptiveInitialTokens) {
                throw BucketExceptions.intervallyAlignedRefillWithAdaptiveInitialTokensIncompatipleWithManualSpecifiedInitialTokens();
            }
            this.initialTokens = initialTokens;
            return this;
        }

        @Override
        public Bandwidth build() {
            return new Bandwidth(capacity, refillPeriodNanos, refillTokens, initialTokens, refillIntervally, timeOfFirstRefillMillis, useAdaptiveInitialTokens, id);
        }

        private void setRefill(long tokens, Duration period, boolean refillIntervally) {
            if (period == null) {
                throw BucketExceptions.nullRefillPeriod();
            }
            if (tokens <= 0) {
                throw BucketExceptions.nonPositivePeriodTokens(tokens);
            }
            long refillPeriodNanos = period.toNanos();
            if (refillPeriodNanos <= 0) {
                throw BucketExceptions.nonPositivePeriod(refillPeriodNanos);
            }
            if (tokens > refillPeriodNanos) {
                throw BucketExceptions.tooHighRefillRate(refillPeriodNanos, tokens);
            }

            this.refillPeriodNanos = refillPeriodNanos;
            this.refillIntervally = refillIntervally;
            this.refillTokens = tokens;
        }

    }

}
