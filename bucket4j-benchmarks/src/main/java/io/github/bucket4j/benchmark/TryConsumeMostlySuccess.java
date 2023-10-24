/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.bucket4j.benchmark;

import io.github.bucket4j.benchmark.state.*;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.profile.GCProfiler;
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

    @Benchmark
    public String tryConsumeOneToken_mostlySuccess_Resilience4j_SemaphoreBasedPermission(Resilience4jState state) {
        return state.semaphoreGuardedSupplier.get();
    }

    @Benchmark
    public String tryConsumeOneToken_mostlySuccess_Resilience4j_AtomicPermission(Resilience4jState state) {
        return state.atomicGuardedSupplier.get();
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
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }

}
