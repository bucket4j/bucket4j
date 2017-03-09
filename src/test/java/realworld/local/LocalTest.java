/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package realworld.local;

import com.github.bucket4j.Bandwidth;
import com.github.bucket4j.Bucket;
import com.github.bucket4j.Bucket4j;
import com.github.bucket4j.local.SynchronizationStrategy;
import org.junit.Test;
import realworld.ConsumptionScenario;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class LocalTest {

    @Test
    public void test15SecondsLockFree() throws Exception {
        Bucket bucket = Bucket4j.builder()
                .addLimit(0, Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .addLimit(0, Bandwidth.simple(200, Duration.ofSeconds(10)))
                .build();

        int threadCount = 4;
        test15SecondsLockFree(bucket, threadCount);
    }

    @Test
    public void test15SecondsSynchronized() throws Exception {
        Bucket bucket = Bucket4j.builder()
                .addLimit(0, Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .addLimit(0, Bandwidth.simple(200, Duration.ofSeconds(10)))
                .build(SynchronizationStrategy.SYNCHRONIZED);

        int threadCount = 4;
        test15SecondsLockFree(bucket, threadCount);
    }

    @Test
    public void test15SecondsUnsafe() throws Exception {
        Bucket bucket = Bucket4j.builder()
                .addLimit(0, Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .addLimit(0, Bandwidth.simple(200, Duration.ofSeconds(10)))
                .build(SynchronizationStrategy.NONE);

        int threadCount = 1;
        test15SecondsLockFree(bucket, threadCount);
    }

    private void test15SecondsLockFree(Bucket bucket, int threadCount) throws Exception {
        ConsumptionScenario scenario = new ConsumptionScenario(threadCount, TimeUnit.SECONDS.toNanos(15), bucket);
        long consumed = scenario.execute();
        long duration = scenario.getDurationNanos();
        System.out.println("Consumed " + consumed + " tokens in the " + duration + " nanos");

        float actualRate = (float) consumed / (float) duration;
        float permittedRate = 200.0f / (float) TimeUnit.SECONDS.toNanos(10);

        String msg = "Actual rate " + actualRate + " is greater then permitted rate " + permittedRate;
        assertTrue(msg, actualRate <= permittedRate);
    }

}
