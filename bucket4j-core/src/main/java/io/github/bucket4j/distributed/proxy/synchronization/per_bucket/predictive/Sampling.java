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

import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.PredictionParameters;

import java.util.Iterator;
import java.util.LinkedList;

public class Sampling {

    private final PredictionParameters predictionParameters;

    private final LinkedList<Sample> samples = new LinkedList<>();
    private double othersRate;

    public Sampling(PredictionParameters predictionParameters) {
        this.predictionParameters = predictionParameters;
    }

    public boolean isNeedToExecuteRemoteImmediately(long currentTimeNanos) {
        while (!samples.isEmpty()) {
            Sample sample = samples.getFirst();
            long sampleAge = currentTimeNanos - sample.syncTimeNanos;
            if (sampleAge > predictionParameters.sampleMaxAgeNanos) {
                samples.removeFirst();
            } else {
                break;
            }
        }
        return othersRate == Double.POSITIVE_INFINITY || samples.size() < predictionParameters.minSamples;
    }

    public long predictedConsumptionByOthersSinceLastSync(long currentTimeNanos) {
        Sample freshSample = samples.getLast();
        long timeSinceLastSync = currentTimeNanos - freshSample.syncTimeNanos;
        if (timeSinceLastSync <= 0 || othersRate == 0.0) {
            return 0L;
        }

        double predictedConsumptionSinceLastSync = othersRate * timeSinceLastSync;
        if (predictedConsumptionSinceLastSync >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return (long) predictedConsumptionSinceLastSync;
    }

    public void rememberRemoteCommandResult(long selfConsumedTokens, long consumedTokensCounter, long now) {
        othersRate = 0.0;
        Sample freshSample = new Sample(now, consumedTokensCounter, selfConsumedTokens);

        Iterator<Sample> samplesIterator = samples.iterator();
        while (samplesIterator.hasNext()) {
            Sample sample = samplesIterator.next();
            if (freshSample.observedConsumptionCounter < sample.observedConsumptionCounter) {
                samplesIterator.remove();
            } else if (now - sample.syncTimeNanos > predictionParameters.sampleMaxAgeNanos) {
                samplesIterator.remove();
            } else if (freshSample.syncTimeNanos < sample.syncTimeNanos) {
                samplesIterator.remove();
            } else if (freshSample.syncTimeNanos == sample.syncTimeNanos) {
                if (sample != samples.getFirst()) {
                    freshSample.selfConsumedTokens += sample.selfConsumedTokens;
                    samplesIterator.remove();
                }
            }
        }

        samples.addLast(freshSample);
        if (samples.size() > predictionParameters.maxSamples) {
            samples.removeFirst();
        } else if (samples.size() < predictionParameters.minSamples) {
            return;
        }

        // let's predict consumption rate in the cluster
        Sample oldestSample = samples.getFirst();
        long tokensSelfConsumedDuringSamplePeriod = 0;
        for (Sample sample : samples) {
            if (sample != oldestSample) {
                tokensSelfConsumedDuringSamplePeriod += sample.selfConsumedTokens;
            }
        }

        long tokensConsumedByOthersDuringSamplingPeriod = freshSample.observedConsumptionCounter - oldestSample.observedConsumptionCounter - tokensSelfConsumedDuringSamplePeriod;
        if (tokensConsumedByOthersDuringSamplingPeriod <= 0) {
            return;
        }

        long timeBetweenSynchronizations = freshSample.syncTimeNanos - oldestSample.syncTimeNanos;
        if (timeBetweenSynchronizations == 0) {
            // should never be there in real cases.
            // cannot divide by zero
            othersRate = Double.POSITIVE_INFINITY;
            return;
        }

        this.othersRate = (double) tokensConsumedByOthersDuringSamplingPeriod / (double) timeBetweenSynchronizations;
    }

    public long getLastSyncTimeNanos() {
        return samples.getLast().syncTimeNanos;
    }

    public void clear() {
        samples.clear();
    }

    private static class Sample {
        private final long syncTimeNanos;
        private final long observedConsumptionCounter;
        private long selfConsumedTokens;

        public Sample(long syncTimeNanos, long observedConsumptionCounter, long selfConsumedTokens) {
            this.syncTimeNanos = syncTimeNanos;
            this.observedConsumptionCounter = observedConsumptionCounter;
            this.selfConsumedTokens = selfConsumedTokens;
        }
    }

}
