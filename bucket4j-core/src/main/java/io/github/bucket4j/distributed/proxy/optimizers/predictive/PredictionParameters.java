package io.github.bucket4j.distributed.proxy.optimizers.predictive;

import java.time.Duration;

public class PredictionParameters {

    private final long maxUnsynchronizedTokens;
    private final long maxUnsynchronizedTimeoutNanos;
    private final boolean predictConsumptionByOtherNodes;
    private final int requiredSamples;
    private final long sampleMaxAgeNanos;

    PredictionParameters(
            long maxUnsynchronizedTokens,
            Duration maxTimeoutBetweenRequests,
            boolean predictConsumptionByOtherNodes,
            int requiredSamples,
            Duration sampleMaxAge
            ) {
        // TODO argument validation
        this.maxUnsynchronizedTokens = maxUnsynchronizedTokens;
        this.maxUnsynchronizedTimeoutNanos = maxTimeoutBetweenRequests.toNanos();
        this.predictConsumptionByOtherNodes = predictConsumptionByOtherNodes;
        this.requiredSamples = requiredSamples;
        this.sampleMaxAgeNanos = sampleMaxAge.toNanos();
    }

    public boolean shouldPredictConsumptionByOtherNodes() {
        return predictConsumptionByOtherNodes;
    }

    public long getMaxUnsynchronizedTokens() {
        return maxUnsynchronizedTokens;
    }

    public long getMaxUnsynchronizedTimeoutNanos() {
        return maxUnsynchronizedTimeoutNanos;
    }

    public int getRequiredSamples() {
        return requiredSamples;
    }

    public long getSampleMaxAgeNanos() {
        return sampleMaxAgeNanos;
    }

}
