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

package io.github.bucket4j.distributed.proxy.synchronization.per_bucket.predictive;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronization;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.DelayParameters;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronizationListener;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.PredictionParameters;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.batch.AsyncBatchingExecutor;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.batch.BatchingExecutor;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.batch.BatchingBucketSynchronization;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.delay.DelayBucketSynchronization;

/**
 * Optimization that can serve requests locally without synchronization with external storage until thresholds are not violated.
 * This optimization is based on top of {@link BatchingBucketSynchronization} and {@link DelayBucketSynchronization},
 * and in additionally this optimization tries to predict aggregated consumption rate in whole cluster in order to reduce the risk of overconsumption that caused by {@link DelayBucketSynchronization}.
 *
 * <p>Usage of this optimization can lead to temporal over-consumption because the synchronization with external storage is performed periodically when thresholds are violated,
 * as well as under-consumption in case of the wrong prediction of aggregated consumption rate.
 *
 * @see DelayParameters
 * @see PredictionParameters
 */
public class PredictiveBucketSynchronization implements BucketSynchronization {

    private final DelayParameters delayParameters;
    private final PredictionParameters predictionParameters;
    private final BucketSynchronizationListener listener;
    private final TimeMeter timeMeter;

    public PredictiveBucketSynchronization(PredictionParameters predictionParameters, DelayParameters delayParameters, BucketSynchronizationListener listener, TimeMeter timeMeter) {
        this.delayParameters = delayParameters;
        this.predictionParameters = predictionParameters;
        this.listener = listener;
        this.timeMeter = timeMeter;
    }

    @Override
    public BucketSynchronization withListener(BucketSynchronizationListener listener) {
        return new PredictiveBucketSynchronization(predictionParameters, delayParameters, listener, timeMeter);
    }

    @Override
    public CommandExecutor apply(CommandExecutor originalExecutor) {
        PredictiveCommandExecutor predictiveCommandExecutor = new PredictiveCommandExecutor(originalExecutor, delayParameters, predictionParameters, listener, timeMeter);
        return new BatchingExecutor(predictiveCommandExecutor, listener);
    }

    @Override
    public AsyncCommandExecutor apply(AsyncCommandExecutor originalExecutor) {
        PredictiveCommandExecutor predictiveCommandExecutor = new PredictiveCommandExecutor(originalExecutor, delayParameters, predictionParameters, listener, timeMeter);
        return new AsyncBatchingExecutor(predictiveCommandExecutor, listener);
    }

}
