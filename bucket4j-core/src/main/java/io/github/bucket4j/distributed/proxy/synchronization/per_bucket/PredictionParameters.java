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
package io.github.bucket4j.distributed.proxy.synchronization.per_bucket;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.predictive.PredictiveBucketSynchronization;

import java.time.Duration;

/**
 * Specifies the parameters for quality of distributes consumption rate prediction that are used by {@link PredictiveBucketSynchronization}
 */
public class PredictionParameters {

    public static final int DEFAULT_MIN_SAMPLES = 2;
    public static final int DEFAULT_MAX_SAMPLES = 10;

    public final int minSamples;
    public final int maxSamples;
    public final long sampleMaxAgeNanos;

    /**
     * Creates new instance of {@link PredictionParameters}
     *
     * @param minSamples the minimum amount of samples that required to make prediction about distributed consumption rate.
     * @param maxSamples the maximum amount of samples to store.
     * @param sampleMaxAge the maximum period of time that sample is stored.
     */
    public PredictionParameters(int minSamples, int maxSamples, Duration sampleMaxAge) {
        this(minSamples, maxSamples, sampleMaxAge.toNanos());
    }

    public PredictionParameters(int minSamples, int maxSamples, long maxUnsynchronizedTimeoutNanos) {
        if (minSamples < 2) {
            throw BucketExceptions.wrongValueOfMinSamplesForPredictionParameters(minSamples);
        }
        this.minSamples = minSamples;

        if (maxSamples < minSamples) {
            throw BucketExceptions.maxSamplesForPredictionParametersCanNotBeLessThanMinSamples(minSamples, maxSamples);
        }
        this.maxSamples = maxSamples;

        if (maxUnsynchronizedTimeoutNanos <= 0) {
            throw BucketExceptions.nonPositiveSampleMaxAgeForPredictionParameters(maxUnsynchronizedTimeoutNanos);
        }
        this.sampleMaxAgeNanos = maxUnsynchronizedTimeoutNanos;
    }

    public static PredictionParameters createDefault(DelayParameters delayParameters) {
        long sampleMaxAge = delayParameters.maxUnsynchronizedTimeoutNanos * 2;
        return new PredictionParameters(DEFAULT_MIN_SAMPLES, DEFAULT_MAX_SAMPLES, sampleMaxAge);
    }

    public int getMinSamples() {
        return minSamples;
    }

    public int getMaxSamples() {
        return maxSamples;
    }

    public long getSampleMaxAgeNanos() {
        return sampleMaxAgeNanos;
    }

}
