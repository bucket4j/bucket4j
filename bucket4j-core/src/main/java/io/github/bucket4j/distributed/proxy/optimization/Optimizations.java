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

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.optimization.batch.BatchingOptimization;
import io.github.bucket4j.distributed.proxy.optimization.predictive.PredictiveOptimization;
import io.github.bucket4j.distributed.proxy.optimization.delay.DelayOptimization;

/**
 * Provides factory methods for all request optimizations that are built-in into Bucket4j library.
 *
 * @see BatchingOptimization
 * @see DelayOptimization
 * @see PredictiveOptimization
 * @see OptimizationListener
 */
public class Optimizations {

    /**
     * Creates optimization that combines independent requests to same bucket into batches in order to reduce request count to remote storage.
     *
     * @return new instance of {@link BatchingOptimization}
     *
     * @see BatchingOptimization
     */
    public static Optimization batching() {
        return new BatchingOptimization(NopeOptimizationListener.INSTANCE);
    }

    /**
     * Creates optimization that can serve requests locally without synchronization with external storage until thresholds are not violated.
     *
     * @param delayParameters thresholds that control whether or not request can be served locally without synchronization with external storage
     *
     * @return new instance of {@link DelayOptimization}
     *
     * @see DelayOptimization
     */
    public static Optimization delaying(DelayParameters delayParameters) {
        return new DelayOptimization(delayParameters, NopeOptimizationListener.INSTANCE, TimeMeter.SYSTEM_MILLISECONDS);
    }

    /**
     * Creates optimization that can serve requests locally without synchronization with external storage until thresholds are not violated,
     * and additionally tries to predict aggregated consumption rate in whole cluster in order to reduce the risk of overconsumption that caused by {@link DelayOptimization}.
     *
     * @param delayParameters thresholds that control whether or not request can be served locally without synchronization with external storage
     * @param predictionParameters parameters that control the quality of prediction of distributed consumption rate
     *
     * @return new instance of {@link PredictiveOptimization}
     *
     * @see PredictiveOptimization
     * @see PredictionParameters
     * @see DelayParameters
     */
    public static Optimization predicting(DelayParameters delayParameters, PredictionParameters predictionParameters) {
        return new PredictiveOptimization(predictionParameters, delayParameters, NopeOptimizationListener.INSTANCE, TimeMeter.SYSTEM_MILLISECONDS);
    }

    /**
     * Has the same semantic as {@link #predicting(DelayParameters, PredictionParameters)} but uses default {@link PredictionParameters}.
     *
     * @param delayParameters thresholds that control whether or not request can be served locally without synchronization with external storage
     *
     * @return new instance of {@link PredictiveOptimization}
     *
     * @see PredictiveOptimization
     * @see PredictionParameters
     */
    public static Optimization predicting(DelayParameters delayParameters) {
        PredictionParameters defaultPrediction = PredictionParameters.createDefault(delayParameters);
        return new PredictiveOptimization(defaultPrediction, delayParameters, NopeOptimizationListener.INSTANCE, TimeMeter.SYSTEM_MILLISECONDS);
    }

}
