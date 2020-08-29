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

public class Optimizations {

    public static Optimization batching() {
        return new BatchingOptimization(NopeOptimizationListener.INSTANCE);
    }

    public static Optimization delaying(DelayParameters delayParameters) {
        return new DelayOptimization(delayParameters, NopeOptimizationListener.INSTANCE, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public static Optimization predicting(DelayParameters delayParameters) {
        PredictionParameters defaultPrediction = PredictionParameters.createDefault(delayParameters);
        return new PredictiveOptimization(defaultPrediction, delayParameters, NopeOptimizationListener.INSTANCE, TimeMeter.SYSTEM_NANOTIME);
    }

    public static Optimization predicting(DelayParameters delayParameters, PredictionParameters predictionParameters) {
        return new PredictiveOptimization(predictionParameters, delayParameters, NopeOptimizationListener.INSTANCE, TimeMeter.SYSTEM_NANOTIME);
    }

}
