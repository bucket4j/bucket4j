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

package io.github.bucket4j.util;

import io.github.bucket4j.Bucket;

import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertTrue;

public class ConsumptionScenario {

    private final CountDownLatch startLatch;
    private final CountDownLatch endLatch;
    private final ConsumerThread[] consumers;
    private final long initializationNanotime;
    private final double permittedRatePerSecond;

    public ConsumptionScenario(int threadCount, long workTimeNanos, Supplier<Bucket> bucketSupplier, Function<Bucket, Long> action, double permittedRatePerSecond) {
        this.startLatch = new CountDownLatch(threadCount);
        this.endLatch = new CountDownLatch(threadCount);
        this.consumers = new ConsumerThread[threadCount];
        this.initializationNanotime = System.nanoTime();
        this.permittedRatePerSecond = permittedRatePerSecond;
        Bucket bucket = bucketSupplier.get();
        for (int i = 0; i < threadCount; i++) {
            this.consumers[i] = new ConsumerThread(startLatch, endLatch, bucket, workTimeNanos, action);
        }
    }

    public void executeAndValidateRate() throws Exception {
        for (ConsumerThread consumer : consumers) {
            consumer.start();
        }
        endLatch.await();

        long durationNanos = System.nanoTime() - initializationNanotime;
        long consumed = 0;
        for (ConsumerThread consumer : consumers) {
            if (consumer.getException() != null) {
                throw consumer.getException();
            } else {
                consumed += consumer.getConsumed();
            }
        }
        
        double actualRatePerSecond = (double) consumed * 1_000_000_000.0d / durationNanos;
        System.out.println("Consumed " + consumed + " tokens in the "
                + durationNanos + " nanos, actualRatePerSecond=" + Formatter.format(actualRatePerSecond)
                + ", permitted rate=" + Formatter.format(permittedRatePerSecond));

        String msg = "Actual rate " + Formatter.format(actualRatePerSecond) + " is greater then permitted rate " + Formatter.format(permittedRatePerSecond);
        assertTrue(msg, actualRatePerSecond <= permittedRatePerSecond);
    }

}
