package io.github.bucket4j.distributed.proxy.optimization;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.Optimization;
import io.github.bucket4j.distributed.proxy.optimization.batch.BatchingOptimization;
import io.github.bucket4j.distributed.proxy.optimization.delay.DelayOptimization;
import io.github.bucket4j.distributed.proxy.optimization.predictive.PredictiveOptimization;

public class Optimizations {

    public static Optimization batched() {
        return new BatchingOptimization();
    }

    public static Optimization delayed(DelayParameters delayParameters) {
        return new DelayOptimization(delayParameters, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public static Optimization predicted(DelayParameters delayParameters) {
        int requiredSamples = 2;
        long sampleMaxAge = delayParameters.maxUnsynchronizedTimeoutNanos * 2;
        PredictionParameters defaultPrediction = new PredictionParameters(requiredSamples, sampleMaxAge);
        return new PredictiveOptimization(defaultPrediction, delayParameters, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public static Optimization predicted(DelayParameters delayParameters, PredictionParameters predictionParameters) {
        return new PredictiveOptimization(predictionParameters, delayParameters, TimeMeter.SYSTEM_MILLISECONDS);
    }

}
