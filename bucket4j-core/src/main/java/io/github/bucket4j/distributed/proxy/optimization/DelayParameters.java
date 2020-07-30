package io.github.bucket4j.distributed.proxy.optimization;

import java.time.Duration;

public class DelayParameters {

    public final long maxUnsynchronizedTokens;
    public final long maxUnsynchronizedTimeoutNanos;

    public DelayParameters(long maxUnsynchronizedTokens, Duration maxTimeoutBetweenRequests) {
        // TODO argument validation
        this.maxUnsynchronizedTokens = maxUnsynchronizedTokens;
        this.maxUnsynchronizedTimeoutNanos = maxTimeoutBetweenRequests.toNanos();
    }

}
