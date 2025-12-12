/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2025 Vladimir Bukhtoyarov
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

package io.github.bucket4j.distributed.proxy;

/**
 * A functional interface that allows custom retry logic for Compare-And-Swap (CAS) operations.
 *
 * <p>
 * This strategy is invoked after each failed CAS attempt to determine whether another retry should be attempted.
 * It provides metadata about the current retry attempt, allowing for sophisticated retry decisions based on:
 * <ul>
 *     <li>Number of attempts made so far</li>
 *     <li>Bucket key (identifier)</li>
 *     <li>Custom business logic (e.g., neural networks, mediator components)</li>
 *     <li>Metrics and monitoring integration</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * // Simple retry limit
 * RetryStrategy limitedRetries = metadata -> metadata.getAttemptNumber() < 5;
 *
 * // Time-based retry
 * RetryStrategy timeBasedRetry = metadata ->
 *     metadata.getElapsedTimeNanos() < Duration.ofSeconds(1).toNanos();
 *
 * // Bucket-specific retry logic with neural network
 * RetryStrategy bucketSpecific = metadata -> {
 *     String bucketKey = (String) metadata.getBucketKey();
 *     return evaluateNeuralModel(bucketKey, metadata.getAttemptNumber());
 * };
 *
 * // Complex business logic
 * RetryStrategy customLogic = metadata -> {
 *     logger.info("CAS retry attempt {} for bucket {} after {}ms",
 *         metadata.getAttemptNumber(),
 *         metadata.getBucketKey(),
 *         metadata.getElapsedTimeNanos() / 1_000_000);
 *     return myMediatorComponent.shouldRetry(metadata);
 * };
 * }</pre>
 *
 * @see ClientSideConfig#withRetryStrategy(RetryStrategy)
 */
@FunctionalInterface
public interface RetryStrategy {

    /**
     * Determines whether another CAS retry attempt should be made.
     *
     * @param metadata information about the current retry attempt
     * @return {@code true} if another retry should be attempted, {@code false} to stop retrying
     */
    boolean shouldRetry(RetryMetadata metadata);

    /**
     * Metadata about a CAS retry attempt.
     */
    class RetryMetadata {
        private final int attemptNumber;
        private final Object bucketKey;
        private final long startTimeNanos;
        private final long currentTimeNanos;

        /**
         * Creates retry metadata.
         *
         * @param attemptNumber the current attempt number (1-based, so first retry is attempt 1)
         * @param bucketKey the bucket identifier/key
         * @param startTimeNanos the time when the first attempt started (in nanoseconds)
         * @param currentTimeNanos the current time (in nanoseconds)
         */
        public RetryMetadata(int attemptNumber, Object bucketKey, long startTimeNanos, long currentTimeNanos) {
            this.attemptNumber = attemptNumber;
            this.bucketKey = bucketKey;
            this.startTimeNanos = startTimeNanos;
            this.currentTimeNanos = currentTimeNanos;
        }

        /**
         * Returns the current attempt number (1-based).
         * The first retry after the initial attempt is attempt 1.
         *
         * @return the current attempt number
         */
        public int getAttemptNumber() {
            return attemptNumber;
        }

        /**
         * Returns the bucket key (identifier) for which the retry is being attempted.
         * This can be used to make bucket-specific retry decisions.
         *
         * <p>
         * The type of the key depends on the ProxyManager implementation:
         * <ul>
         *     <li>String for most implementations (Redis, Hazelcast, etc.)</li>
         *     <li>Custom types for specialized implementations</li>
         * </ul>
         *
         * @return the bucket key
         */
        public Object getBucketKey() {
            return bucketKey;
        }

        /**
         * Returns the time when the first attempt started (in nanoseconds).
         *
         * @return the start time in nanoseconds
         */
        public long getStartTimeNanos() {
            return startTimeNanos;
        }

        /**
         * Returns the current time (in nanoseconds).
         *
         * @return the current time in nanoseconds
         */
        public long getCurrentTimeNanos() {
            return currentTimeNanos;
        }

        /**
         * Returns the elapsed time since the first attempt (in nanoseconds).
         *
         * @return the elapsed time in nanoseconds
         */
        public long getElapsedTimeNanos() {
            return currentTimeNanos - startTimeNanos;
        }
    }
}

