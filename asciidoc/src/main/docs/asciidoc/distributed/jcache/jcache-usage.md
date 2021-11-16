# JCache integration
``Bucket4j`` supports any GRID solution which compatible with [JCache API (JSR 107)](https://www.jcp.org/en/jsr/detail?id=107) specification. 

**Do not forget to read** [JCache production checklist](production-jcache-checklist.md) **before using the Bucket4j over JCache cluster.**

To use JCache extension you also need to add following dependency:
``xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-jcache</artifactId>
    <version>${bucket4j.version}</version>
</dependency>
`` 

JCache expects javax.cache.cache-api to be a provided dependency. Do not forget to add following dependency:
``xml
<dependency>
    <groupId>javax.cache</groupId>
    <artifactId>cache-api</artifactId>
    <version>${jcache.version}</version>
</dependency>
``

## Example 1 - limiting access to HTTP server by IP address
Imagine that you develop any Servlet based WEB application and want to limit access per IP basis.
You want to use same limits for each IP - 30 requests per minute.

ServletFilter would be obvious place to check limits:
``java
public class IpThrottlingFilter implements javax.servlet.Filter {
    
    private static final BucketConfiguration configuration = Bucket4j.configurationBuilder()
                                                              .addLimit(Bandwidth.simple(30, Duration.ofMinutes(1)))
                                                              .build();
    
    // cache for storing token buckets, where IP is key.
    @Inject
    private javax.cache.Cache<String, GridBucketState> cache;
    
    private ProxyManager<String> buckets;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
         // init bucket registry
         buckets = Bucket4j.extension(JCache.class).proxyManagerForCache(cache);
    }
    
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String ip = IpHelper.getIpFromRequest(httpRequest);
        
        // acquire cheap proxy to bucket  
        Bucket bucket = buckets.getProxy(ip, configuration);

        // tryConsume returns false immediately if no tokens available with the bucket
        if (bucket.tryConsume(1)) {
            // the limit is not exceeded
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            // limit is exceeded
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setContentType("text/plain");
            httpResponse.setStatus(429);
            httpResponse.getWriter().append("Too many requests");
        }
    }

}
``

## Example 2 - limiting access to service by contract agreements
Imagine that you provides paid language translation service via HTTP.
Each your user has unique agreement which differs from each other. 
Details of each agreement is stored in relational database, and takes significant time to fetch(for example 100ms). 
The example above will not work fine in this case, because time to create/fetch configuration of bucket from database
will be 100 times slower than limit-checking itself.
Bucket4j solves this problem via lazy configuration suppliers which are called if and only if bucket was not yet stored in grid,
thus it is possible to implement solution  that will read agreement from database once per each user. 

``java
public class IpThrottlingFilter implements javax.servlet.Filter {

    // service to provide per user limits
    @Inject
    private LimitProvider limitProvider;
    
    // cache for storing token buckets, where IP is key.
    @Inject
    private javax.cache.Cache<String, GridBucketState> cache;
    
    private ProxyManager<String> buckets;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
         // init bucket registry
         buckets = Bucket4j.extension(JCache.class).proxyManagerForCache(cache);
    }
    
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String userId = AutentificationHelper.getUserIdFromRequest(httpRequest);
        
        // prepare configuration supplier which will be called(on first interaction with proxy) if bucket was not saved yet previously. 
        Supplier<BucketConfiguration> configurationLazySupplier = getConfigSupplierForUser(userId);
        
        // acquire cheap proxy to bucket  
        Bucket bucket = buckets.getProxy(ip, configurationLazySupplier);

        // tryConsume returns false immediately if no tokens available with the bucket
        if (bucket.tryConsume(1)) {
            // the limit is not exceeded
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            // limit is exceeded
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setContentType("text/plain");
            httpResponse.setStatus(429);
            httpResponse.getWriter().append("Too many requests");
        }
    }
    
    private Supplier<BucketConfiguration> getConfigSupplierForUser(String userId) {
         return () -> {
             long translationsPerDay = limitProvider.readPerDayLimitFromAgreementsDatabase(userId);
             return Bucket4j.configurationBuilder()
                         .addLimit(Bandwidth.simple(translationsPerDay, Duration.ofDays(1)))
                         .build();
         };
    }

}
``


**Question:** is the provided JCache integration safe across multiple JVMs? Does it ensure that two nodes creating a bucket simultaneously on a given Cache<K, V> will only actually create one single bucket (without resetting a previously created one with the same key)?  
**Answer:** Yes. JCache integration is safe for multi node environment, Bucket4j never replaces bucket which already exists.
This behavior is guaranteed by **putIfAbsent** method contract of [javax.cache.Cache](http://static.javadoc.io/javax.cache/cache-api/1.0.0/javax/cache/Cache.html) class.

**Question:** Does ProxyManager store buckets internally, could be this a reason of OutOfMemoryError?  
**Answer:** No. ProxyManager stores nothing about buckets which it returns, the buckets actually stored in in-memory GRID outside client JVM.
Think about proxy returned by ``ProxyManager#getBucket`` just about very cheap pointer to data which actually stored somewhere outside.
So, independently of count of buckets ProxyManager will never be a reason of crash or extreme memory consumption.

**Question:** what will happen if bucket state will be lost in the GRID  because of split-brain, human mistake or pragmatically errors introduced by GRID vendor?  
**Answer:** ProxyManager automatically detect this kind of situations and save bucket yet another time, to reconstruct bucket it uses provided configuration supplier.
Reconstructed bucket remembers nothing about previously consumed tokens, so limit can be exceeded in this kind of GRID failures.

**Question:** should I always work with JCache through ProxyManager?  
**Answer:** It depends. When you have deal with potentially huge and unpredictable amount of buckets, it is always better to use ProxyManager.
ProxyManager protects you from common performance pitfalls(like described in [this issue](https://github.com/vladimir-bukhtoyarov/bucket4j/issues/26)).
But when you have deal with one or few buckets which well known at development time, then it would be better to avoid ProxyManager 
and work directly with [GridBucket](https://github.com/vladimir-bukhtoyarov/bucket4j/blob/2.0/bucket4j-core/src/main/java/io/github/bucket4j/grid/GridBucket.java) as described in the next example.
 
## Example 3 - working with JCache without ProxyManager abstraction
Imagine yet another time that you develop WEB application and want to protect the whole cluster by 1000 requests per second, independently from request source,
in other words you need one single bucket which protects the system at whole. Lets create ServletFilter to check limits similar to ``Example 1``:
``java
public class GlobalThrottlingFilter implements javax.servlet.Filter {

    private static final String BUCKET_ID = "global-limit";
    
    @Inject
    private javax.cache.Cache<String, GridBucketState> cache;
    
    private Bucket bucket;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
         // create bucket
         bucket = Bucket4j.extension(JCache.class).builder()
             .addLimit(Bandwidth.simple(1000, Duration.ofSeconds(1)))
             .build(cache, BUCKET_ID, RecoveryStrategy.RECONSTRUCT);
    }
    
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // tryConsume returns false immediately if no tokens available with the bucket
        if (bucket.tryConsume(1)) {
            // the limit is not exceeded
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            // limit is exceeded
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setContentType("text/plain");
            httpResponse.setStatus(429);
            httpResponse.getWriter().append("Too many requests");
        }
    }

}
``
As you can see the code is simpler when you work with Bucket directly without ProxyManager, so use this way always when all buckets are known at development time. 

## Runnable examples of JCache integration
Bucket4j well tested with ``Hazelcast`` and ``Apache Ignite/GridGain``, you can use integration tests from [this folder](https://github.com/vladimir-bukhtoyarov/bucket4j/tree/2.0/bucket4j-jcache/src/test/java/io/github/bucket4j/grid/jcache) as live examples.

## Why JCache specification is not enough and since 3.0 were introduced the dedicated modules for Infinispan, Hazelcast and Ignite?
Asynchronous processing is very important for high-throughput applications, but JCache specification does not specify asynchronous API, because two early attempts to bring this kind functionality at spec level [307](https://github.com/jsr107/jsr107spec/issues/307),[312](https://github.com/jsr107/jsr107spec/issues/312) were failed in absence of consensus.
Sad, but true, if you need for asynchronous API, then JCache extension is useless, and you need to choose from following extensions:
- [bucket4j-ignite](ignite.md)
- [bucket4j-hazelcast](hazelcast.md)
- [bucket4j-infinispan](infinispan.md)

Of course implementation the asynchronous support for any other JCache provider outside from the list above should be easy exercise, 
so feel free to return back the pull request addressed to cover your favorite JCache provider.

# Production checklist especially in the context of distributed systems
Before using Bucket4j in clustered scenario you need to understand, agree and configure following points:

#### Do not forget about exception handling
When working within a distributed system, it is innevitable that requests may cross the border of the current JVM, leading to a communication on the network.
Network being unreliable, it is impossible to avoid failures. Thus you should embrace this reality and be ready to get unchecked exceptions when interacting with a distributed bucket.
**It is your responsibility to handle(or ignore) such exceptions:**
* You probably do not want to fail business transactions if the grid responsible for throttling goes down. If this is the case you can simply log the exception and continue your business transaction without throttling
* If you wish to fail your business transaction when the grid responsible for throttling goes down, simply rethrow or don't catch the exception

#### Do not forget to configure backups
If the state of any bucket should survive the restart/crash of grid node that holds its state, you need to configure backups yourself, in way specific to the particular grid vendor. For example, see how to [configure backups for Apache Ignite](https://apacheignite.readme.io/v2.3/docs/primary-and-backup-copies).

#### Retention tuning is your responsibility
When dealing with multi-tenant scenarios like a bucket per user or a bucket per IP address,
the amount of buckets in the cache will continuously increase. This is because a new bucket will be created each time a new key is detected.
To prevent exhausting the available memory of your cluster you need to configure following aspects:
* **Expiration since last access** - in order to allow the grid to remove the keys which haven't been used in a long time. For example, see how to [configure expiration policy for Apache Ignite](https://apacheignite.readme.io/docs/expiry-policies).
* **Maximum cache size(in units or bytes)** - Obviously it is preferable to lose bucket data than lose the whole cluster due to out of memory exception.

#### High availability(HA) tuning and testing is your responsibility
There are no special settings for HA supported by Bucket4j, because Bucket4j does nothing more that just invoking EntryProcessors on the cache.
Instead Bucket4j relies on *you* to configure the cache with proper parameters that control redundancy and high availability.

Years of experience working with distributed system has tought the author that High Availability does not come for free. You need to test and verify that your system remains available. This cannot be provided by this or any other library. Your system will most certainly go down if you do not plan for that.

#### Verification of compatibility with particular JCache provider is your responsibility
Keep in mind that there are many non-certified implementations of JCache specification on the market.
Many of them want to increase their popularity by declaring support for the JCache API,
but often only the API is supported and the semantic of JCache is totally ignored.
Usage Bucket4j with this kind of libraries should be completely avoided.

Bucket4j is only compatible with implementations which obey the JCache specification rules(especially related to EntryProcessor execution).
Oracle Coherence, Apache Ignite, Hazelcast are good examples of safe implementations of JCache.

Because it is impossible to test all possible JCache providers, you need to test your provider by yourself.
Just run this code in order to be sure that your implementation of JCache provides good isolation for EntryProcessors
``java
import javax.cache.Cache;
import javax.cache.processor.EntryProcessor;
import java.util.concurrent.CountDownLatch;
import java.io.Serializable;

public class CompatibilityTest {

    final Cache<String, Integer> cache;


    public CompatibilityTest(Cache<String, Integer> cache) {
        this.cache = cache;
    }

    public void test() throws InterruptedException {
        String key = "42";
        int threads = 4;
        int iterations = 1000;
        cache.put(key, 0);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        EntryProcessor<String, Integer, Void> processor = (EntryProcessor<String, Integer, Void> & Serializable) (mutableEntry, objects) -> {
                            int value = mutableEntry.getValue();
                            mutableEntry.setValue(value + 1);
                            return null;
                        };
                        cache.invoke(key, processor);
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        int value = cache.get(key);
        if (value == threads * iterations) {
            System.out.println("Implementation which you use is compatible with Bucket4j");
        } else {
            String msg = "Implementation which you use is not compatible with Bucket4j";
            msg += ", " + (threads * iterations - value) + " writes are missed";
            throw new IllegalStateException(msg);
        }
    }

}
``
The check does 4000 increments of integer in parallel and verifies that no one update has been missed.
If check passed then your JCache provider is compatible with Bucket4j, the throttling will work fine in distributed and concurrent environment.
If check is not passed, then reach to the particular JCache provider team and consult why its implementation misses the writes.