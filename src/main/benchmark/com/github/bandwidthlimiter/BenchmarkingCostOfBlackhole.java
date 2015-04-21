package com.github.bandwidthlimiter;

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
public class BenchmarkingCostOfBlackhole {

    @Benchmark
    public long baseline() {
        return Thread.currentThread().getId();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BenchmarkingCostOfBlackhole.class.getSimpleName())
                .warmupIterations(2)
                .measurementIterations(3)
                .threads(1)
                .forks(1)
                .build();

        new Runner(opt).run();
    }

}
