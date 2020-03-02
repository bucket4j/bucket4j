package io.github.bucket4j;

import io.github.bucket4j.state.GuavaLimiterState;
import io.github.bucket4j.state.LocalLockFreeState;
import io.github.bucket4j.state.LocalSynchronizedState;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class TryConsumeMostlySuccess {

    @Benchmark
    public boolean tryConsumeOneToken_mostlySuccess_LockFree(LocalLockFreeState state) {
        return state.unlimitedBucket.tryConsume(1);
    }

    @Benchmark
    public boolean tryConsumeOneToken_mostlySuccess_Synchronized(LocalSynchronizedState state) {
        return state.unlimitedBucket.tryConsume(1);
    }

    @Benchmark
    public boolean tryConsumeOneToken_mostlySuccess_GuavaLimiter(GuavaLimiterState state) {
        return state.guavaRateLimiter.tryAcquire();
    }

    public static class OneThread {

        public static void main(String[] args) throws RunnerException {
            benchmark(1);
        }

    }

    public static class TwoThreads {

        public static void main(String[] args) throws RunnerException {
            benchmark(2);
        }

    }

    public static class FourThreads {

        public static void main(String[] args) throws RunnerException {
            benchmark(4);
        }

    }

    private static void benchmark(int threadCount) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(TryConsumeMostlySuccess.class.getSimpleName())
                .warmupIterations(10)
                .measurementIterations(10)
                .threads(threadCount)
                .forks(1)
                .build();

        new Runner(opt).run();
    }

}
