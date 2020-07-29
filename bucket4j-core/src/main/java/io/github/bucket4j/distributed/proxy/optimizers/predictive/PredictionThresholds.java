package io.github.bucket4j.distributed.proxy.optimizers.predictive;

import java.time.Duration;

public class PredictionThresholds {

    private final long maxUnsynchronizedTokens;
    private final long maxUnsynchronizedTimeoutNanos;

    private PredictionThresholds(long maxUnsynchronizedTokens, Duration maxTimeoutBetweenRequests) {
        // TODO argument validation
        this.maxUnsynchronizedTokens = maxUnsynchronizedTokens;
        this.maxUnsynchronizedTimeoutNanos = maxTimeoutBetweenRequests.toNanos();
    }

    public long getMaxUnsynchronizedTokens() {
        return maxUnsynchronizedTokens;
    }

    public long getMaxUnsynchronizedTimeoutNanos() {
        return maxUnsynchronizedTimeoutNanos;
    }

}
