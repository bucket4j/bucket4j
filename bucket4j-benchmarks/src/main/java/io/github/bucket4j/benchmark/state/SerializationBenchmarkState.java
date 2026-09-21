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
package io.github.bucket4j.benchmark.state;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.MathType;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteStat;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.distributed.serialization.SerializationStyle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.time.Duration;

@State(Scope.Benchmark)
public class SerializationBenchmarkState {

    public final Version version = Versions.getLatest();

    public final RemoteBucketState remoteBucketState = createRemoteBucketState();

    public final byte[] dataOutputBytes = InternalSerializationHelper.serializeState(remoteBucketState, version, SerializationStyle.DATA_OUTPUT);
    public final byte[] byteBufferBytes = InternalSerializationHelper.serializeState(remoteBucketState, version, SerializationStyle.BYTE_BUFFER);

    private static RemoteBucketState createRemoteBucketState() {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.classic(200, Refill.greedy(200, Duration.ofSeconds(10))))
                .build();
        BucketState bucketState = BucketState.createInitialState(configuration, MathType.INTEGER_64_BITS, System.nanoTime());
        bucketState.addTokens(500);
        return new RemoteBucketState(bucketState, new RemoteStat(1_000), null);
    }

}
