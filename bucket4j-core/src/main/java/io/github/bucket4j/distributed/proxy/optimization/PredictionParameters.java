package io.github.bucket4j.distributed.proxy.optimization;

import java.time.Duration;

public class PredictionParameters {

    public final int requiredSamples;
    public final long sampleMaxAgeNanos;

    public PredictionParameters(int requiredSamples, Duration sampleMaxAge) {
        this(requiredSamples, sampleMaxAge.toNanos());
    }

    public PredictionParameters(int requiredSamples, long maxUnsynchronizedTimeoutNanos) {
        // TODO argument validation
        this.requiredSamples = requiredSamples;
        this.sampleMaxAgeNanos = maxUnsynchronizedTimeoutNanos;
    }

}
