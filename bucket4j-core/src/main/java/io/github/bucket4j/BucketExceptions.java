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

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;

public final class BucketExceptions {

    // ------------------- construction time exceptions --------------------------------
    public static IllegalArgumentException nonPositiveCapacity(long capacity) {
        String pattern = "{0} is wrong value for capacity, because capacity should be positive";
        String msg = MessageFormat.format(pattern, capacity);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositiveInitialTokens(long initialTokens) {
        String pattern = "{0} is wrong value for initial capacity, because initial tokens count should be positive";
        String msg = MessageFormat.format(pattern, initialTokens);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullBandwidth() {
        String msg = "Bandwidth can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullBuilder() {
        String msg = "Bandwidth builder can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullBandwidthRefill() {
        String msg = "Bandwidth refill can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullTimeMeter() {
        String msg = "Time meter can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullSynchronizationStrategy() {
        String msg = "Synchronization strategy can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullListener() {
        String msg = "listener can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullRefillPeriod() {
        String msg = "Refill period can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullFixedRefillInterval() {
        String msg = "Fixed refill interval can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullScheduler() {
        String msg = "Scheduler can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullConfiguration() {
        String msg = "Configuration can not be null";
        return new IllegalArgumentException(msg);
    }

    public static Throwable nullConfigurationFuture() {
        String msg = "Configuration future can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullConfigurationSupplier() {
        String msg = "Configuration supplier can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositivePeriod(long period) {
        String pattern = "{0} is wrong value for period of bandwidth, because period should be positive";
        String msg = MessageFormat.format(pattern, period);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositiveLimitToSync(long unsynchronizedPeriod) {
        String pattern = "{0} is wrong value for limit to sync, because period should be positive";
        String msg = MessageFormat.format(pattern, unsynchronizedPeriod);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositiveFixedRefillInterval(Duration fixedRefillInterval) {
        String pattern = "{0} is wrong value for fixed refill interval, because period should be positive";
        String msg = MessageFormat.format(pattern, fixedRefillInterval);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositivePeriodTokens(long tokens) {
        String pattern = "{0} is wrong value for period tokens, because tokens should be positive";
        String msg = MessageFormat.format(pattern, tokens);
        return new IllegalArgumentException(msg);
    }

    // TODO add test
    public static IllegalArgumentException nonPositiveTokensForDelayParameters(long maxUnsynchronizedTokens) {
        String pattern = "{0} is wrong value for maxUnsynchronizedTokens, because tokens should be positive";
        String msg = MessageFormat.format(pattern, maxUnsynchronizedTokens);
        return new IllegalArgumentException(msg);
    }

    // TODO add test
    public static IllegalArgumentException nullMaxTimeoutBetweenSynchronizationForDelayParameters() {
        String msg = "maxTimeoutBetweenSynchronization can not be null";
        return new IllegalArgumentException(msg);
    }

    // TODO add test
    public static IllegalArgumentException nonPositiveMaxTimeoutBetweenSynchronizationForDelayParameters(Duration maxTimeoutBetweenSynchronization) {
        String pattern = "maxTimeoutBetweenSynchronization = {0}, maxTimeoutBetweenSynchronization can not be negative";
        String msg = MessageFormat.format(pattern, maxTimeoutBetweenSynchronization);
        return new IllegalArgumentException(msg);
    }

    // TODO add test
    public static IllegalArgumentException wrongValueOfMinSamplesForPredictionParameters(int minSamples) {
        String pattern = "minSamples = {0}, minSamples must be >= 2";
        String msg = MessageFormat.format(pattern, minSamples);
        return new IllegalArgumentException(msg);
    }

    // TODO add test
    public static IllegalArgumentException maxSamplesForPredictionParametersCanNotBeLessThanMinSamples(int minSamples, int maxSamples) {
        String pattern = "minSamples = {0}, maxSamples = {1}, maxSamples must be >= minSamples";
        String msg = MessageFormat.format(pattern, minSamples, maxSamples);
        return new IllegalArgumentException(msg);
    }

    // TODO add test
    public static IllegalArgumentException nonPositiveSampleMaxAgeForPredictionParameters(long maxUnsynchronizedTimeoutNanos) {
        String pattern = "maxUnsynchronizedTimeoutNanos = {0}, maxUnsynchronizedTimeoutNanos must be positive";
        String msg = MessageFormat.format(pattern, maxUnsynchronizedTimeoutNanos);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException restrictionsNotSpecified() {
        String msg = "At list one limited bandwidth should be specified";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException tooHighRefillRate(long periodNanos, long tokens) {
        double actualRate = (double) tokens / (double) periodNanos;
        String pattern = "{0} token/nanosecond is not permitted refill rate" +
                ", because highest supported rate is 1 token/nanosecond";
        String msg = MessageFormat.format(pattern, actualRate);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositiveTimeOfFirstRefill(Instant timeOfFirstRefill) {
        String pattern = "{0} is wrong value for timeOfFirstRefill, because timeOfFirstRefill should be a positive date";
        String msg = MessageFormat.format(pattern, timeOfFirstRefill);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException intervallyAlignedRefillWithAdaptiveInitialTokensIncompatipleWithManualSpecifiedInitialTokens() {
        String msg = "Intervally aligned Refill With adaptive initial tokens incompatiple with maanual specified initial tokens";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException intervallyAlignedRefillCompatibleOnlyWithWallClock() {
        String msg = "intervally aligned refill is compatible only with wall-clock style TimeMeter";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException foundTwoBandwidthsWithSameId(int firstIndex, int secondIndex, String id) {
        String pattern = "All identifiers must unique. Id: {0}, first index: {1}, second index: {2}";
        String msg = MessageFormat.format(pattern, id, firstIndex, secondIndex);
        return new IllegalArgumentException(msg);
    }

    // ------------------- end of construction time exceptions --------------------------------

    // ------------------- usage time exceptions  ---------------------------------------------
    public static IllegalArgumentException nonPositiveNanosToWait(long waitIfBusyNanos) {
        String pattern = "Waiting value should be positive, {0} is wrong waiting period";
        String msg = MessageFormat.format(pattern, waitIfBusyNanos);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositiveTokensToConsume(long tokens) {
        String pattern = "Unable to consume {0} tokens, due to number of tokens to consume should be positive";
        String msg = MessageFormat.format(pattern, tokens);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositiveTokensLimitToSync(long tokens) {
        String pattern = "Sync threshold tokens should be positive, {0} is wrong waiting period";
        String msg = MessageFormat.format(pattern, tokens);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException reservationOverflow() {
        String msg = "Existed hardware is unable to service the reservation of so many tokens";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullTokensInheritanceStrategy() {
        String msg = "Tokens migration mode must not be null";
        return new IllegalArgumentException(msg);
    }

    public static BucketExecutionException executionException(Throwable cause) {
        return new BucketExecutionException(cause);
    }

    public static UnsupportedOperationException asyncModeIsNotSupported() {
        String msg = "Asynchronous mode is not supported";
        return new UnsupportedOperationException(msg);
    }

    public static UnsupportedOperationException expirationAfterWriteIsNotSupported() {
        String msg = "Expiration-after-write is not supported";
        return new UnsupportedOperationException(msg);
    }

    public static IllegalArgumentException nonPositiveRequestTimeout(Duration requestTimeout) {
        String msg = "Non-positive request timeout " + requestTimeout;
        return new IllegalArgumentException(msg);
    }

    public static TimeoutException timeoutReached(long nanosElapsed, long requestTimeoutNanos) {
        String pattern = "Timeout {0} nanos has been reached, actual operation time is {1} nanos";
        String msg = MessageFormat.format(pattern, requestTimeoutNanos, nanosElapsed);
        return new TimeoutException(msg, nanosElapsed, requestTimeoutNanos);
    }

    public static IllegalArgumentException isNotWallBasedClockUsedInDistributedEnvironment(Class<? extends TimeMeter> clockClass) {
        String pattern = "Trying to use not wall-based clock {0} in distributed environment";
        String msg = MessageFormat.format(pattern, clockClass);
        return new IllegalArgumentException(msg);
    }

    public static BucketExecutionException from(Throwable t) {
        if (t instanceof BucketExecutionException) {
            return  (BucketExecutionException) t;
        }
        return new BucketExecutionException(t);
    }

    public static class BucketExecutionException extends RuntimeException {
        public BucketExecutionException(Throwable cause) {
            super(cause);
        }
        public BucketExecutionException(String message) {
            super(message);
        }
    }

    private BucketExceptions() {
        // private constructor for utility class
    }

}
