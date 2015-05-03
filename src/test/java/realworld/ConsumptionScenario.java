package realworld;

import com.github.bandwidthlimiter.bucket.Bucket;

import java.util.concurrent.CountDownLatch;

public class ConsumptionScenario {

    private final CountDownLatch startLatch;
    private final CountDownLatch endLatch;
    private final ConsumerThread[] consumers;
    private long duration;

    public ConsumptionScenario(int threadCount, long workTimeNanos, Bucket bucket) {
        this.startLatch = new CountDownLatch(threadCount);
        this.endLatch = new CountDownLatch(threadCount);
        this.consumers = new ConsumerThread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            this.consumers[i] = new ConsumerThread(startLatch, endLatch, bucket, workTimeNanos);
        }
    }

    public long execute() throws Exception {
        long start = System.nanoTime();

        for (ConsumerThread consumer : consumers) {
            consumer.start();
        }
        endLatch.await();
        this.duration = System.nanoTime() - start;

        long consumed = 0;
        for (ConsumerThread consumer : consumers) {
            if (consumer.getException() != null) {
                throw consumer.getException();
            } else {
                consumed += consumer.consumed;
            }
        }


        return consumed;
    }

    public long getDurationNanos() {
        return duration;
    }

}
