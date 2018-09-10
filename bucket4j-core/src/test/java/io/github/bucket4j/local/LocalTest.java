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

package io.github.bucket4j.local;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.UninterruptibleBlockingStrategy;
import io.github.bucket4j.util.ConsumptionScenario;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class LocalTest {

    private LocalBucketBuilder builder = Bucket4j.builder()
            .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)).withInitialTokens(0))
            .addLimit(Bandwidth.simple(200, Duration.ofSeconds(10)).withInitialTokens(0));

    private double permittedRatePerSecond = Math.min(1_000d / 60, 200.0 / 10);

    @Test
    public void testTryConsume_lockFree() throws Exception {
        int threadCount = 4;
        Function<Bucket, Long> action = b -> b.tryConsume(1)? 1L : 0L;
        test15Seconds(() -> builder.build(), threadCount, action);
    }

    @Test
    public void testTryConsume_lockFree_Limited() throws Exception {
        int threadCount = 4;
        Function<Bucket, Long> action = b -> b.asScheduler().tryConsumeUninterruptibly(1, Duration.ofMillis(50))? 1L : 0L;
        test15Seconds(() -> builder.build(), threadCount, action);
    }

    @Test
    public void testTryConsume_Synchronized() throws Exception {
        int threadCount = 4;
        Function<Bucket, Long> action = b -> b.tryConsume(1)? 1L : 0L;
        test15Seconds(() -> builder.withSynchronizationStrategy(SynchronizationStrategy.SYNCHRONIZED).build(), threadCount, action);
    }

    @Test
    public void testTryConsume_SynchronizedLimited() throws Exception {
        int threadCount = 4;
        Function<Bucket, Long> action = b -> b.asScheduler().tryConsumeUninterruptibly(1, Duration.ofMillis(50), UninterruptibleBlockingStrategy.PARKING)? 1L : 0L;
        test15Seconds(() -> builder.withSynchronizationStrategy(SynchronizationStrategy.SYNCHRONIZED).build(), threadCount, action);
    }

    @Test
    public void testTryConsume_Unsafe() throws Exception {
        int threadCount = 1;
        Function<Bucket, Long> action = b -> b.tryConsume(1)? 1L : 0L;
        test15Seconds(() -> builder.withSynchronizationStrategy(SynchronizationStrategy.NONE).build(), threadCount, action);
    }

    @Test
    public void testTryConsume_UnsafeLimited() throws Exception {
        int threadCount = 1;
        Function<Bucket, Long> action = b -> b.asScheduler().tryConsumeUninterruptibly(1, TimeUnit.MILLISECONDS.toNanos(50))? 1L: 0L;
        test15Seconds(() -> builder.withSynchronizationStrategy(SynchronizationStrategy.NONE).build(), threadCount, action);
    }

    private void test15Seconds(Supplier<Bucket> bucket, int threadCount, Function<Bucket, Long> action) throws Exception {
        ConsumptionScenario scenario = new ConsumptionScenario(threadCount, TimeUnit.SECONDS.toNanos(15), bucket, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

}
