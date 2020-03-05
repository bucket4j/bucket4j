
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
    private final long initializationTimeMillis;
    private final double permittedRatePerSecond;

    public ConsumptionScenario(int threadCount, long workTimeNanos, Supplier<Bucket> bucketSupplier, Function<Bucket, Long> action, double permittedRatePerSecond) {
        this.startLatch = new CountDownLatch(threadCount);
        this.endLatch = new CountDownLatch(threadCount);
        this.consumers = new ConsumerThread[threadCount];
        this.initializationTimeMillis = System.currentTimeMillis();
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
        long durationMillis = System.currentTimeMillis() - initializationTimeMillis;

        long consumed = 0;
        for (ConsumerThread consumer : consumers) {
            if (consumer.getException() != null) {
                throw consumer.getException();
            } else {
                consumed += consumer.getConsumed();
            }
        }

        double actualRatePerSecond = (double) consumed * 1_000.0d / durationMillis;
        System.out.println("Consumed " + consumed + " tokens in the "
                + durationMillis + " millis, actualRatePerSecond=" + Formatter.format(actualRatePerSecond)
                + ", permitted rate=" + Formatter.format(permittedRatePerSecond));

        String msg = "Actual rate " + Formatter.format(actualRatePerSecond) + " is greater then permitted rate " + Formatter.format(permittedRatePerSecond);
        assertTrue(msg, actualRatePerSecond <= permittedRatePerSecond);
    }

}
