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
/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j.benchmark.state;

import java.time.Duration;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.local.ConcurrencyStrategy;

@State(Scope.Benchmark)
public class LocalUnsafeState_ieee754 {

    public final Bucket bucket = Bucket.builder()
            .withMillisecondPrecision()
            .addLimit(
                Bandwidth.builder().capacity(Long.MAX_VALUE / 2).refillGreedy(Long.MAX_VALUE / 2, Duration.ofNanos(Long.MAX_VALUE / 2)).build()
            )
            .withSynchronizationStrategy(ConcurrencyStrategy.UNSAFE)
            .build();


    public final Bucket bucketWithoutRefill = Bucket.builder()
            .withMillisecondPrecision()
            .withCustomTimePrecision(new TimeMeter() {
                @Override
                public long currentTimeNanos() {
                    return 0;
                }

                @Override
                public boolean isWallClockBased() {
                    return false;
                }
            })
            .addLimit(
                Bandwidth.builder().capacity(Long.MAX_VALUE / 2).refillGreedy(Long.MAX_VALUE / 2, Duration.ofNanos(Long.MAX_VALUE / 2)).build()
            )
            .withSynchronizationStrategy(ConcurrencyStrategy.UNSAFE)
            .build();


}
