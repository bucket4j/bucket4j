import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.optimization.Optimizations;
import io.github.bucket4j.grid.coherence.CoherenceProxyManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.littlegrid.ClusterMemberGroup;
import org.littlegrid.ClusterMemberGroupUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spock.lang.Specification;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class EarlyThrottlingInvestigationTest {

    private static final Logger logger = LoggerFactory.getLogger(EarlyThrottlingInvestigationTest.class);

    private static ClusterMemberGroup memberGroup;
    private static NamedCache cache;
    private static CoherenceProxyManager<Object> proxyManager;

    @BeforeClass
    public static void prepareCache() throws InterruptedException {
        memberGroup = ClusterMemberGroupUtils.newBuilder()
                .setStorageEnabledCount(0)
                .buildAndConfigureFor(ClusterMemberGroup.BuildAndConfigureEnum.STORAGE_ENABLED_MEMBER);

        cache = CacheFactory.getCache("my_buckets");
        proxyManager = new CoherenceProxyManager<>(cache);
    }

    @AfterClass
    public static void shutdownCache() {
        ClusterMemberGroupUtils.shutdownCacheFactoryThenClusterMemberGroups(memberGroup);
    }

    @Test(timeout = 120_000)
    public void testEarlyTrottling() throws InterruptedException {
        for (boolean useBatchingOptimization : Arrays.asList(true, false)) {
            for (boolean reduceAvailableTokensOnCreate: Arrays.asList(true, false)) {
                logger.info("\n\n\n");
                logger.info("Beginning test reduceAvailableTokensOnCreate={} useBatchingOptimization={}", reduceAvailableTokensOnCreate, useBatchingOptimization);

                int bucketCapacity = 60_000;
                int refillTokens = 60_000;
                Duration refillPeriod = Duration.ofSeconds(1);
                Instant firstRefillTime = Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(1, ChronoUnit.SECONDS);

                Refill refill = Refill.intervallyAligned(refillTokens, refillPeriod, firstRefillTime, reduceAvailableTokensOnCreate);
                BucketConfiguration configuration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.classic(bucketCapacity, refill))
                        .build();
                String key = UUID.randomUUID().toString();
                Bucket bucket =  useBatchingOptimization ?
                        proxyManager.builder().withOptimization(Optimizations.batching()).build(key, configuration) :
                        proxyManager.builder().build(key, configuration);
                long amountOfTokensAtCreationTime = bucket.getAvailableTokens();
                logger.info("At the creation time bucket has {} tokens", amountOfTokensAtCreationTime);

                long startNanotime = System.nanoTime();

                // consume 900_000 tokens plus all tokens that were at creation time
                long toConsumeTokens = 900_000 + amountOfTokensAtCreationTime;
                final AtomicLong consumedTokens = new AtomicLong();

                int parallelThreadsCount = 4;
                CountDownLatch startLatch = new CountDownLatch(parallelThreadsCount);
                CountDownLatch endLatch = new CountDownLatch(parallelThreadsCount);
                for (int i = 0; i < parallelThreadsCount; i++) {
                    new Thread(() -> {
                        try {
                            startLatch.countDown();
                            startLatch.await();
                            while (consumedTokens.get() < toConsumeTokens) {
                                long toConsumeOnIteration = ThreadLocalRandom.current().nextLong(10) + 1;
                                toConsumeOnIteration = Math.min(toConsumeTokens - consumedTokens.get(), toConsumeOnIteration);
                                if (toConsumeOnIteration <= 0) {
                                    break;
                                }
                                long actuallyConsumed = bucket.tryConsumeAsMuchAsPossible(toConsumeOnIteration);
                                if (actuallyConsumed > 0) {
                                    consumedTokens.addAndGet(actuallyConsumed);
                                }
                            }
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        } finally {
                            endLatch.countDown();
                        }
                    }).start();
                }
                endLatch.await();

                long testDurationNanos = System.nanoTime() - startNanotime;
                BigDecimal consumptionRate = new BigDecimal((double) consumedTokens.get() / (double) testDurationNanos * 1_000_000_000);
                logger.info("{} tokens was consumed in {} milliseconds, consumption rate is {} tokens per second", consumedTokens, TimeUnit.NANOSECONDS.toMillis(testDurationNanos), consumptionRate);
                logger.info("\n\n\n");
            }
        }
    }


}
