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
package io.github.bucket4j.distributed.proxy.optimization;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.proxy.optimization.delay.DelayOptimization;

import java.time.Duration;

/**
 * Describes parameters for {@link DelayOptimization}.
 *
 * @see DelayOptimization
 * @see Optimizations#delaying(DelayParameters)
 */
public class DelayParameters {

    public final long maxUnsynchronizedTokens;
    public final long maxUnsynchronizedTimeoutNanos;

    /**
     * Creates the new instance of {@link DelayParameters}
     *
     * @param maxUnsynchronizedTokens threshold that describes how many tokens can be consumed locally without synchronization with external storage. Must be a positive number.
     * @param maxTimeoutBetweenSynchronization threshold that describes how long bucket proxy can act locally without synchronization with external storage. Must be a positive duration.
     */
    public DelayParameters(long maxUnsynchronizedTokens, Duration maxTimeoutBetweenSynchronization) {
        this.maxUnsynchronizedTokens = maxUnsynchronizedTokens;
        if (maxUnsynchronizedTokens <= 0) {
            throw BucketExceptions.nonPositiveTokensForDelayParameters(maxUnsynchronizedTokens);
        }
        if (maxTimeoutBetweenSynchronization == null) {
            throw BucketExceptions.nullMaxTimeoutBetweenSynchronizationForDelayParameters();
        }
        if (maxTimeoutBetweenSynchronization.isNegative() || maxTimeoutBetweenSynchronization.isZero()) {
            throw BucketExceptions.nonPositiveMaxTimeoutBetweenSynchronizationForDelayParameters(maxTimeoutBetweenSynchronization);
        }
        this.maxUnsynchronizedTimeoutNanos = maxTimeoutBetweenSynchronization.toNanos();
    }

}
