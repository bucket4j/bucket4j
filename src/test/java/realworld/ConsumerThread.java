package realworld;

import com.github.bandwidthlimiter.bucket.Bucket;

import java.util.concurrent.CountDownLatch;

public class ConsumerThread extends Thread {

    final CountDownLatch startLatch;
    final CountDownLatch endLatch;
    final Bucket bucket;
    final long workTimeNanos;
    long consumed;
    Exception exception;

    public ConsumerThread(CountDownLatch startLatch, CountDownLatch endLatch, Bucket bucket, long workTimeNanos) {
        this.startLatch = startLatch;
        this.endLatch = endLatch;
        this.bucket = bucket;
        this.workTimeNanos = workTimeNanos;
    }

    @Override
    public void run() {
        try {
            startLatch.countDown();
            startLatch.await();
            long startTime = System.nanoTime();
            do {
                if (bucket.tryConsumeSingleToken()) {
                    consumed++;
                }
            } while (System.nanoTime() - startTime < workTimeNanos);
        } catch (Exception e) {
            exception = e;
        } finally {
            endLatch.countDown();
        }
    }

    public Exception getException() {
        return exception;
    }

}
