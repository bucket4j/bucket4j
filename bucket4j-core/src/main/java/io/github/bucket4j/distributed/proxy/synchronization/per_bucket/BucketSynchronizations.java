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

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.batch.BatchingBucketSynchronization;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.predictive.PredictiveBucketSynchronization;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.delay.DelayBucketSynchronization;

/**
 * Provides factory methods for all request optimizations that are built-in into Bucket4j library.
 *
 * @see BatchingBucketSynchronization
 * @see DelayBucketSynchronization
 * @see PredictiveBucketSynchronization
 * @see BucketSynchronizationListener
 */
public class BucketSynchronizations {

    /**
     * Creates optimization that combines independent requests to same bucket into batches in order to reduce request count to remote storage.
     *
     * @return new instance of {@link BatchingBucketSynchronization}
     *
     * @see BatchingBucketSynchronization
     */
    public static BucketSynchronization batching() {
        return new BatchingBucketSynchronization(NopeSynchronizationListener.INSTANCE);
    }

    /**
     * Creates optimization that can serve requests locally without synchronization with external storage until thresholds are not violated.
     *
     * @param delayParameters thresholds that control whether request can be served locally without synchronization with external storage
     *
     * @return new instance of {@link DelayBucketSynchronization}
     *
     * @see DelayBucketSynchronization
     */
    public static BucketSynchronization delaying(DelayParameters delayParameters) {
        return new DelayBucketSynchronization(delayParameters, NopeSynchronizationListener.INSTANCE, TimeMeter.SYSTEM_MILLISECONDS);
    }

    /**
     * Creates optimization that can serve requests locally without synchronization with external storage until thresholds are not violated,
     * and additionally tries to predict aggregated consumption rate in whole cluster in order to reduce the risk of overconsumption that caused by {@link DelayBucketSynchronization}.
     *
     * @param delayParameters thresholds that control whether request can be served locally without synchronization with external storage
     * @param predictionParameters parameters that control the quality of prediction of distributed consumption rate
     *
     * @return new instance of {@link PredictiveBucketSynchronization}
     *
     * @see PredictiveBucketSynchronization
     * @see PredictionParameters
     * @see DelayParameters
     */
    public static BucketSynchronization predicting(DelayParameters delayParameters, PredictionParameters predictionParameters) {
        return new PredictiveBucketSynchronization(predictionParameters, delayParameters, NopeSynchronizationListener.INSTANCE, TimeMeter.SYSTEM_MILLISECONDS);
    }

    /**
     * Has the same semantic as {@link #predicting(DelayParameters, PredictionParameters)} but uses default {@link PredictionParameters}.
     *
     * @param delayParameters thresholds that control whether request can be served locally without synchronization with external storage
     *
     * @return new instance of {@link PredictiveBucketSynchronization}
     *
     * @see PredictiveBucketSynchronization
     * @see PredictionParameters
     */
    public static BucketSynchronization predicting(DelayParameters delayParameters) {
        PredictionParameters defaultPrediction = PredictionParameters.createDefault(delayParameters);
        return new PredictiveBucketSynchronization(defaultPrediction, delayParameters, NopeSynchronizationListener.INSTANCE, TimeMeter.SYSTEM_MILLISECONDS);
    }

}
