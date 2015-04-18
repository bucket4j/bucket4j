package com.github.bandwidthlimiter;

import com.github.bandwidthlimiter.bucket.Bucket;
import com.google.common.util.concurrent.RateLimiter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class CompareWithGuavaBenchmark {

    @State(Scope.Benchmark)
    public static class LocalThreadSafeState {
        private final Bucket bucket = Limiters
                .withNanoTimePrecision().withLimitedBandwidth(Long.MAX_VALUE / 2, Long.MAX_VALUE / 2)
                .buildLocalThreadSafe();
    }
    @Benchmark
    public boolean benchmarkLocalThreadSafe(LocalThreadSafeState state) {
        return state.bucket.tryConsumeSingleToken();
    }

    @State(Scope.Benchmark)
    public static class GuavaLimiterState {
        private final RateLimiter rateLimiter = RateLimiter.create(Long.MAX_VALUE/2);
    }
    @Benchmark
    public boolean benchmarkGuavaLimiter(GuavaLimiterState state) {
        return state.rateLimiter.tryAcquire();
    }

}
