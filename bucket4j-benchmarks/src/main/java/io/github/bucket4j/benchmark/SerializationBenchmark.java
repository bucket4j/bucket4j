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

import io.github.bucket4j.benchmark.state.SerializationBenchmarkState;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.distributed.serialization.SerializationStyle;
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

/**
 * Compares {@link io.github.bucket4j.distributed.serialization.DataOutputSerializationAdapter} (legacy,
 * stream-based) against {@link io.github.bucket4j.distributed.serialization.ByteBufferSerializationAdapter}
 * (estimate-then-serialize into a right-sized {@link java.nio.ByteBuffer}) for {@code RemoteBucketState}
 * serialization/deserialization, both in latency and allocations(run with {@link GCProfiler}).
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class SerializationBenchmark {

    @Benchmark
    public byte[] serializeState_dataOutput(SerializationBenchmarkState state) {
        return InternalSerializationHelper.serializeState(state.remoteBucketState, state.version, SerializationStyle.DATA_OUTPUT);
    }

    @Benchmark
    public byte[] serializeState_byteBuffer(SerializationBenchmarkState state) {
        return InternalSerializationHelper.serializeState(state.remoteBucketState, state.version, SerializationStyle.BYTE_BUFFER);
    }

    @Benchmark
    public RemoteBucketState deserializeState_dataOutput(SerializationBenchmarkState state) {
        return InternalSerializationHelper.deserializeState(state.dataOutputBytes, SerializationStyle.DATA_OUTPUT);
    }

    @Benchmark
    public RemoteBucketState deserializeState_byteBuffer(SerializationBenchmarkState state) {
        return InternalSerializationHelper.deserializeState(state.byteBufferBytes, SerializationStyle.BYTE_BUFFER);
    }

    public static class OneThread {

        public static void main(String[] args) throws RunnerException {
            benchmark(1);
        }

    }

    public static class FourThreads {

        public static void main(String[] args) throws RunnerException {
            benchmark(4);
        }

    }

    private static void benchmark(int threadCount) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SerializationBenchmark.class.getSimpleName())
                .warmupIterations(10)
                .measurementIterations(10)
                .threads(threadCount)
                .forks(1)
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }

}
