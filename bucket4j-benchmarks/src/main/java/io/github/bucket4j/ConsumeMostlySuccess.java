/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

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
public class ConsumeMostlySuccess {

    @Benchmark
    public void consumeOneToken_mostlySuccess_LockFree(LocalLockFreeState state) {
        state._10_milion_rps_Bucket.asScheduler().tryConsumeUninterruptibly(1, TimeUnit.MILLISECONDS.toNanos(1), UninterruptibleBlockingStrategy.PARKING);
    }

    @Benchmark
    public void consumeOneToken_mostlySuccess_Synchronized(LocalSynchronizedState state) {
        state._10_milion_rps_Bucket.asScheduler().tryConsumeUninterruptibly(1, TimeUnit.MILLISECONDS.toNanos(1), UninterruptibleBlockingStrategy.PARKING);
    }

    @Benchmark
    public void consumeOneToken_mostlySuccess_GuavaLimiter(GuavaLimiterState state) {
        state._10_milion_rps_RateLimiter.acquire();
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
                .include(ConsumeMostlySuccess.class.getSimpleName())
                .warmupIterations(10)
                .measurementIterations(10)
                .threads(threadCount)
                .forks(1)
                .build();

        new Runner(opt).run();
    }

}
