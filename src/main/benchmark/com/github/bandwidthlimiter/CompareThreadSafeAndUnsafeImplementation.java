package com.github.bandwidthlimiter;

import com.github.bandwidthlimiter.bucket.Bucket;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class CompareThreadSafeAndUnsafeImplementation {

    @State(Scope.Benchmark)
    public static class LocalUnsafeState {
        private final Bucket bucket = Limiters
            .withNanoTimePrecision().withLimitedBandwidth(Long.MAX_VALUE / 2, Long.MAX_VALUE / 2)
            .buildLocalUnsafe();
    }
    @Benchmark
    public boolean benchmarkLocalUnsafe(LocalUnsafeState state) {
        return state.bucket.tryConsumeSingleToken();
    }



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

}
