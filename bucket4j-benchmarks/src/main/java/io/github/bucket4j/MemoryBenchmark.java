package io.github.bucket4j;

import com.google.common.util.concurrent.RateLimiter;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import org.openjdk.jol.info.ClassLayout;

public class MemoryBenchmark {

    public static void main(String[] args) {
        System.out.println("Bucket4j: " + ClassLayout.parseClass(Bucket.class).toPrintable());
        System.out.println("Guava: " + ClassLayout.parseClass(RateLimiter.class).toPrintable());
        System.out.println("Resilience4j: " + ClassLayout.parseClass(AtomicRateLimiter.class).toPrintable());
        System.out.println("Resilience4j.semaphoreBasedRateLimiter: " + ClassLayout.parseClass(io.github.resilience4j.ratelimiter.RateLimiter.class).toPrintable());
    }

}
