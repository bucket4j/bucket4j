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
