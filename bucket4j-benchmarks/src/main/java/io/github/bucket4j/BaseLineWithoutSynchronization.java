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

import io.github.bucket4j.state.LocalUnsafeState;
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
public class BaseLineWithoutSynchronization {

    @Benchmark
    public boolean tryConsumeOneToken_alwaysSuccess(LocalUnsafeState state) {
        return state.bucket.tryConsume(1);
    }

    @Benchmark
    public boolean tryConsumeOneToken_alwaysSuccess_withoutRefill(LocalUnsafeState state) {
        return state.bucketWithoutRefill.tryConsume(1);
    }

    @Benchmark
    public long baseLineGetCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static class OneThread {

        public static void main(String[] args) throws RunnerException {
            Options opt = new OptionsBuilder()
                    .include(BaseLineWithoutSynchronization.class.getSimpleName())
                    .warmupIterations(10)
                    .measurementIterations(10)
                    .threads(1)
                    .forks(1)
                    .build();

            new Runner(opt).run();
        }

    }

}
