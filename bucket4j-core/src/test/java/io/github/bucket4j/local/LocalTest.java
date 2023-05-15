
package io.github.bucket4j.local;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.UninterruptibleBlockingStrategy;
import io.github.bucket4j.util.ConsumptionScenario;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class LocalTest {

    private LocalBucketBuilder builder = Bucket.builder()
            .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)).withInitialTokens(0))
            .addLimit(Bandwidth.simple(200, Duration.ofSeconds(10)).withInitialTokens(0));

    private double permittedRatePerSecond = Math.min(1_000d / 60, 200.0 / 10);

    @Test
    public void testTryConsume_lockFree() throws Throwable {
        int threadCount = 4;
        Function<Bucket, Long> action = b -> b.tryConsume(1)? 1L : 0L;
        testScenario(() -> builder.build(), threadCount, action);
    }

    @Test
    public void testTryConsume_lockFree_Limited() throws Throwable {
        int threadCount = 4;
        Function<Bucket, Long> action = b -> b.asBlocking().tryConsumeUninterruptibly(1, Duration.ofMillis(50))? 1L : 0L;
        testScenario(() -> builder.build(), threadCount, action);
    }

    @Test
    public void testTryConsume_Synchronized() throws Throwable {
        int threadCount = 4;
        Function<Bucket, Long> action = b -> b.tryConsume(1)? 1L : 0L;
        testScenario(() -> builder.withSynchronizationStrategy(SynchronizationStrategy.SYNCHRONIZED).build(), threadCount, action);
    }

    @Test
    public void testTryConsume_SynchronizedLimited() throws Throwable {
        int threadCount = 4;
        Function<Bucket, Long> action = b -> b.asBlocking().tryConsumeUninterruptibly(1, Duration.ofMillis(50), UninterruptibleBlockingStrategy.PARKING)? 1L : 0L;
        testScenario(() -> builder.withSynchronizationStrategy(SynchronizationStrategy.SYNCHRONIZED).build(), threadCount, action);
    }

    @Test
    public void testTryConsume_Unsafe() throws Throwable {
        int threadCount = 1;
        Function<Bucket, Long> action = b -> b.tryConsume(1)? 1L : 0L;
        testScenario(() -> builder.withSynchronizationStrategy(SynchronizationStrategy.NONE).build(), threadCount, action);
    }

    @Test
    public void testTryConsume_UnsafeLimited() throws Throwable {
        int threadCount = 1;
        Function<Bucket, Long> action = b -> b.asBlocking().tryConsumeUninterruptibly(1, TimeUnit.MILLISECONDS.toNanos(50))? 1L: 0L;
        testScenario(() -> builder.withSynchronizationStrategy(SynchronizationStrategy.NONE).build(), threadCount, action);
    }

    private void testScenario(Supplier<Bucket> bucket, int threadCount, Function<Bucket, Long> action) throws Throwable {
        int durationSeconds = System.getenv("CI") == null ? 5 : 1;
        ConsumptionScenario scenario = new ConsumptionScenario(threadCount, TimeUnit.SECONDS.toNanos(durationSeconds), bucket, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

}
