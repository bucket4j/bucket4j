package io.github.bucket4j.grid.jcache.ecache;


import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;

import org.ehcache.jsr107.EhcacheCachingProvider;

import io.github.bucket4j.grid.jcache.CompatibilityTest;

public class EhcacheCompatibilityInvestigation {

    public static void main(String[] args) throws InterruptedException {
        CachingProvider provider = Caching.getCachingProvider(EhcacheCachingProvider.class.getName());
        CacheManager cacheManager = provider.getCacheManager();
        MutableConfiguration<String, Integer> configuration =
                new MutableConfiguration<String, Integer>()
                        .setTypes(String.class, Integer.class)
                        .setStoreByValue(false)
                        .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ONE_MINUTE));
        Cache<String, Integer> cache = cacheManager.createCache("jCache", configuration);
        cache.put("any", 42);

        CompatibilityTest test = new CompatibilityTest(cache);
        test.test();
    }

}
