package io.github.bucket4j.redis.redisson.cas;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.grid.jcache.JCacheProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.redisson.config.Config;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.testcontainers.containers.GenericContainer;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class RedissonBasedProxyManagerTest extends AbstractDistributedBucketTest<String> {

    private static GenericContainer container;
    private static ProxyManager<String> proxyManager;

    @BeforeClass
    public static void setup() {
        container = startRedisContainer();
        proxyManager = createProxyManager(container);
    }


    @AfterClass
    public static void shutdown() {
        if (container != null) {
            container.close();
        }
    }

    @Test
    public void test_Issue_279() {
        int MIN_CAPACITY = 4;
        int MAX_CAPACITY = 10;
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(MIN_CAPACITY, Refill.intervally(4, Duration.ofMinutes(20))))
                .addLimit(Bandwidth.classic(MAX_CAPACITY, Refill.intervally(10, Duration.ofMinutes(60))))
                .build();

        Bucket bucket = proxyManager.builder().build("something", configuration);

        for (int i = 1; i <= 4; i++) {
            VerboseResult<ConsumptionProbe> verboseResult = bucket.asVerbose().tryConsumeAndReturnRemaining(1);
            ConsumptionProbe probe = verboseResult.getValue();
            long[] availableTokensPerEachBandwidth = verboseResult.getDiagnostics().getAvailableTokensPerEachBandwidth();
            System.out.println("Remaining tokens = " + probe.getRemainingTokens());
            System.out.println("Tokens per bandwidth = " + Arrays.toString(availableTokensPerEachBandwidth));
            assertEquals(MIN_CAPACITY - i, probe.getRemainingTokens());
            assertEquals(MIN_CAPACITY - i, availableTokensPerEachBandwidth[0]);
            assertEquals(MAX_CAPACITY - i, availableTokensPerEachBandwidth[1]);
        }
    }

    private static ProxyManager<String> createProxyManager(GenericContainer container) {
        String redisAddress = container.getContainerIpAddress();
        Integer redisPort = container.getMappedPort(6379);
        String redisUrl = "redis://" + redisAddress + ":" + redisPort;

        Config config = new Config();
        config.useSingleServer().setAddress(redisUrl);

        CacheManager cacheManager = cacheManager(config);
        return createJCacheProxyManager(cacheManager);
    }

    private static CacheManager cacheManager(Config config) {
        var cacheManager = Caching.getCachingProvider().getCacheManager();

        var jcacheConfig = new MutableConfiguration<>();

        jcacheConfig.setExpiryPolicyFactory(
                CreatedExpiryPolicy.factoryOf(new javax.cache.expiry.Duration(TimeUnit.MINUTES, 60)));

        var redissonConfig = RedissonConfiguration.fromConfig(config, jcacheConfig);

        cacheManager.createCache("bucket", redissonConfig);

        return cacheManager;
    }

    private static ProxyManager<String> createJCacheProxyManager(CacheManager cacheManager) {
        return new JCacheProxyManager<>(cacheManager.getCache("bucket"));
    }

    private static GenericContainer startRedisContainer() {
        GenericContainer genericContainer = new GenericContainer("redis:4.0.11")
                .withExposedPorts(6379);
        genericContainer.start();
        return genericContainer;
    }

    @Override
    protected ProxyManager<String> getProxyManager() {
        return proxyManager;
    }

    @Override
    protected String generateRandomKey() {
        return UUID.randomUUID().toString();
    }

}
