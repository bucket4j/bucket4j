package com.github.bandwidthlimiter;

import com.github.bandwidthlimiter.state.LocalThreadSafeNanotimePrecisionState;
import com.github.bandwidthlimiter.state.LocalUnsafeNanotimePrecisionState;
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
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class BenchmarkingThreadSafeAndUnsafeImplementationForCornerCaseOfSingleThread {

    @Benchmark
    public boolean benchmarkLocalThreadSafe(LocalThreadSafeNanotimePrecisionState state) {
        return state.bucket.tryConsumeSingleToken();
    }

    @Benchmark
    public boolean benchmarkLocalUnsafe(LocalUnsafeNanotimePrecisionState state) {
        return state.bucket.tryConsumeSingleToken();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BenchmarkingThreadSafeAndUnsafeImplementationForCornerCaseOfSingleThread.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .threads(1)
                .forks(1)
                .build();

        new Runner(opt).run();
    }

}
