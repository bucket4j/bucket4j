package com.github.bandwidthlimiter;

import com.github.bandwidthlimiter.state.GuavaNanotimePrecisionLimiterState;
import com.github.bandwidthlimiter.state.LocalThreadSafeNanotimePrecisionState;
import com.github.bandwidthlimiter.state.ThreadDistributionState;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class BenchmarkingWithGuavaRateLimiter {

    @Benchmark
    public double baseline(ThreadDistributionState threadLocalCounter) {
        return threadLocalCounter.invocationCount++;
    }

    @Benchmark
    public boolean benchmarkLocalThreadSafe(LocalThreadSafeNanotimePrecisionState state, ThreadDistributionState threadLocalCounter) {
        boolean result = state.bucket.tryConsumeSingleToken();
        threadLocalCounter.invocationCount++;
        return result;
    }

    @Benchmark
    public boolean benchmarkGuavaLimiter(GuavaNanotimePrecisionLimiterState state, ThreadDistributionState threadLocalCounter) {
        boolean result = state.guavaRateLimiter.tryAcquire();
        threadLocalCounter.invocationCount++;
        return result;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BenchmarkingWithGuavaRateLimiter.class.getSimpleName())
                .warmupIterations(10)
                .measurementIterations(10)
                .threads(4)
                .forks(1)
                .build();

        new Runner(opt).run();
    }

}
