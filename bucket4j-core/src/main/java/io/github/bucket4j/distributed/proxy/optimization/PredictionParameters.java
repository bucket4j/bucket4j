package io.github.bucket4j.distributed.proxy.optimization;

import java.time.Duration;

public class PredictionParameters {

    public static final int DEFAULT_MIN_SAMPLES = 2;
    public static final int DEFAULT_MAX_SAMPLES = 10;

    public final int minSamples;
    public final int maxSamples;
    public final long sampleMaxAgeNanos;

    public PredictionParameters(int minSamples, int maxSamples, Duration sampleMaxAge) {
        this(minSamples, maxSamples, sampleMaxAge.toNanos());
    }

    public PredictionParameters(int minSamples, int maxSamples, long maxUnsynchronizedTimeoutNanos) {
        // TODO argument validation
        this.minSamples = minSamples;
        this.maxSamples = maxSamples;
        this.sampleMaxAgeNanos = maxUnsynchronizedTimeoutNanos;
    }

    public static PredictionParameters createDefault(DelayParameters delayParameters) {
        long sampleMaxAge = delayParameters.maxUnsynchronizedTimeoutNanos * 2;
        return new PredictionParameters(DEFAULT_MIN_SAMPLES, DEFAULT_MAX_SAMPLES, sampleMaxAge);
    }

    public int getMinSamples() {
        return minSamples;
    }

    public int getMaxSamples() {
        return maxSamples;
    }

    public long getSampleMaxAgeNanos() {
        return sampleMaxAgeNanos;
    }

}
