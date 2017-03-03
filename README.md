## Bucket4j - is a java implementation of token/leaky bucket algorithm for rate limiting
[![Build Status](https://travis-ci.org/vladimir-bukhtoyarov/bucket4j.svg?branch=master)](https://travis-ci.org/vladimir-bukhtoyarov/bucket4j)
[![Coverage Status](https://coveralls.io/repos/vladimir-bukhtoyarov/bucket4j/badge.svg)](https://coveralls.io/r/vladimir-bukhtoyarov/bucket4j)
[![Join the chat at https://gitter.im/vladimir-bukhtoyarov/bucket4j](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vladimir-bukhtoyarov/bucket4j?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

### Algorithm in high level

The token bucket algorithm can be conceptually understood as follows:

* A token is added to the bucket every `1/r` seconds.
* The bucket can hold at the most `b` tokens. If a token arrives when the bucket is full, it is discarded.
* When trying to consume `n` tokens, `n` tokens are removed from bucket.
* If fewer than `n` tokens are available, then consumption is disallowed.

See for more details:

* [Wikipedia - Token Bucket](http://en.wikipedia.org/wiki/Token_bucket)
* [Wikipedia - Leaky Bucket](http://en.wikipedia.org/wiki/Leaky_bucket)
* [Wikipedia - Generic cell rate algorithm](http://en.wikipedia.org/wiki/Generic_cell_rate_algorithm)

### Advantages of Bucket4j

* Implemented around ideas of well known family of algorithms, which are by de facto standard for rate limiting in the IT industry.
* Effective lock free implementation without any critical section, Bucket4j is good scalable for multithreading case.
* Absolutely non-compromise precision, Bucket4j does not operate with floats or doubles, all calculation are performed in the integer arithmetic,
this feature protects end users from calculation errors involved by rounding.
* Rich API:
  * More then one bandwidth per bucket. For example you can limit 1000 events per hours but not often then 100 events per minute. 
  * Customizable time measurement. You are able to specify how to time will be measurement: as `System.nanotime()` or `System.currentTimeMillis()`
or you can to specify own way to measure time.
  * Ability to have guaranteed bandwidth. Think about guaranteed bandwidth like a 911 number which you can to call when no money on your balance.
  * Dynamic capacity. If needs capacity of the bucket can be changed over time.
* Ability to switch from one JVM to cluster in two lines of code. Using Bucket4j you are able to limiting something in the cluster of JVMs.
Since [release 1.2](https://github.com/vladimir-bukhtoyarov/bucket4j/releases/tag/1.2.0) the ```Bucket4j``` supports any GRID solution which compatible with [JCache API (JSR 107)](https://www.jcp.org/en/jsr/detail?id=107) specification.
Just use your favorite grid including [Hazelcast](http://hazelcast.com/products/hazelcast/), [Ignite](http://www.oracle.com/technetwork/middleware/coherence/overview/index-087514.html), [Coherence](http://www.oracle.com/technetwork/middleware/coherence/overview/index.html), [Infinispan](http://infinispan.org/) or any other.  

### Get Bucket4j library

#### By direct link
[Download compiled jar, sources, javadocs](https://github.com/vladimir-bukhtoyarov/bucket4j/releases/tag/1.2.0)

#### You can build Bucket4j from sources

```bash
git clone https://github.com/vladimir-bukhtoyarov/bucket4j.git
cd bucket4j
mvn clean install
```

#### You can add Bucket4j to your project as maven dependency

The bucket4j library is distributed through [Bintray](http://bintray.com/), so you need to add Bintray repository to your `pom.xml`

```xml
     <repositories>
         <repository>
             <id>jcenter</id>
             <url>http://jcenter.bintray.com</url>
         </repository>
     </repositories>
```

Then include Bucket4j as dependency to your `pom.xml`

```xml
<dependency>
    <groupId>com.github</groupId>
    <artifactId>bucket4j</artifactId>
    <version>1.2.0</version>
</dependency>
```

### Basic usage

#### Simple example

Imagine that you develop WEB application and want to limit user to access for application no often then 10 times for second:

```java
import com.github.bucket4j.Bucket4j;

public class ThrottlingFilter implements javax.servlet.Filter {
    
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpSession session = httpRequest.getSession(true);

        Bucket bucket = (Bucket) session.getAttribute("throttler");
        if (bucket == null) {
            // build bucket with required capacity and associate it with particular user
            bucket = Bucket4j.builder()
                .withLimitedBandwidth(10, Duration.ofSeconds(1))
                .build();
            session.setAttribute("throttler", bucket);
        }

        // tryConsumeSingleToken returns false immediately if no tokens available with the bucket
        if (bucket.tryConsumeSingleToken()) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setContentType("text/plain");
            httpResponse.setStatus(429);
            httpResponse.getWriter().append("Too many requests");
        }
    }

}
```
 

#### Yet another simple example

Suppose you have a piece of code that polls a website and you would only like to be able to access the site 100 time per minute: 

```java
import com.github.bucket4j.Bucket4j;

// Create a token bucket with required capacity.
Bucket bucket = Bucket4j.builder()
                .withLimitedBandwidth(100, Duration.ofMinutes(1))
                .build();

// ...
while (true) {
  // Consume a token from the token bucket.  
  // If a token is not available this method will block until
  // the refill adds one to the bucket.
  bucket.consume(1);

  poll();
}
```

### Advanced usage

#### Example of multiple bandwidth

Imagine that you are developing load testing tool, in order to be ensure that testable system is able to dispatch 1000 requests per 1 minute.
But you do not want to randomly kill the testable system by generation all 1000 events in one second instead of 1 minute. 
To solve problem you can construct following bucket:
```java
import com.github.bucket4j.Bucket4j;

Bucket bucket = Bucket4j.builder()
       // allows 10000 tokens per 1 minute
       .withLimitedBandwidth(10000, Duration.ofMinutes(1))
       // but not often then 50 tokens per 1 second
       .withLimitedBandwidth(50, Duration.ofSeconds(1))
       .build();

// ...
while (true) {
  // Consume a token from the token bucket.  If a token is not available this method will block until
  // the refill adds one to the bucket.
  bucket.consume(1);

  workloadExecutor.execute(new LoadTask());
}
```

#### Example of guaranteed bandwidth

Let's imagine that you develop mailing server. 
In order to prevent spam, you want to restrict user to send emails not often than by 1000 times per hour. 
But in same time, you want to provide guarantee to user that will be able to send 1 email each 10 minute even in the case where the limit by hour is exceeded. 
In this case you can construct bucket like this:

```java
import com.github.bucket4j.Bucket4j;

Bucket bucket = Bucket4j.builder()
    .withLimitedBandwidth(1000, Duration.ofHours(1))
    .withGuaranteedBandwidth(1, Duration.ofMinutes(10))
    .build();
```

#### Using initial capacity  

By default initial size of bucket is equal to capacity. 
But sometimes, you may want to have lesser initial size, for example for case of cold start in order to prevent denial of service. 
You can specify initial size as third parameter during bandwidth construction:

```java

int initialCapacity = 42;

Bucket bucket = Bucket4j.builder()
    .withLimitedBandwidth(1000, initialCapacity, Duration.ofHours(1))
    .build();
```

#### Using dynamic capacity  

Sometimes, you may want to have a bucket with dynamic capacity. For example if you want to have capacity 10 per 1 minute for daily time,
and 2 per minute for nightly time, then construct bucket like this

```java

BandwidthAdjuster adjuster = new BandwidthAdjuster() {
    @Override
    public long getCapacity(long currentTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTime);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 7 && hour <= 23) {
            return 10;    
        } else {
            return 2;
        }
    }
};
Bucket4j.builder()
    .withLimitedBandwidth(adjuster, 10, Duration.ofMinutes(1));
```

#### Customizing time measurement
##### Choosing nanotime time resolution
By default Bucket4j uses millisecond time resolution, it is preferred time measurement strategy. 
But rarely(for example benchmarking) you wish the nanosecond precision:
``` java
Bucket4j.builder().withNanosecondPrecision()
```
Be very careful to choose this time measurement strategy, because ```System.nanoTime()``` produces inaccurate results, 
use this strategy only if period of bandwidth is too small that millisecond resolution will be undesired.
   
##### Specify custom time measurement strategy
You can specify your custom time meter, if existing miliseconds or nanotime time meters is not enough for your purposes. For example:

```java

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class CurrentThreadCpuTimeMeter implements TimeMeter {

    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

    @Override
    public long currentTimeNanos() {
        return THREAD_MX_BEAN.getCurrentThreadCpuTime();
    }

}

Bucket bucket = Bucket4j.builder()
                .withCustomTimePrecision(new CurrentThreadCpuTimeMeter())
                .withLimitedBandwidth(100, Duration.ofMinutes(1))
                .build();


```

### Examples of distributed usage
```Bucket4j``` supports any GRID solution which compatible with [JCache API (JSR 107)](https://www.jcp.org/en/jsr/detail?id=107) specification.
The distributed usage scenario is little bit more complicated than simple usage inside one JVM, 
because it is need specify the reaction which should be applied in case of bucket state is lost by any reason, for example because of:
- Split-brain happen.
- The bucket state was stored on single grid node without replication strategy and this node was crashed.
- Wrong cache configuration.
- Pragmatically errors introduced by GRID vendor.
- Human mistake.

The ```Bucket4j``` make the client to specify recovery strategy from the list:
- **RECONSTRUCT** Initialize bucket yet another time. Use this strategy if availability is more preferred than consistency.
- **THROW_BUCKET_NOT_FOUND_EXCEPTION** Throw BucketNotFoundException. Use this strategy if consistency is more preferred than availability. 

#### Example of Hazelcast integration 
``` java
  
Config config = new Config();
CacheSimpleConfig cacheConfig = new CacheSimpleConfig();
cacheConfig.setName("my_buckets");
config.addCacheConfig(cacheConfig);

hazelcastInstance = Hazelcast.newHazelcastInstance(config);
ICacheManager cacheManager = hazelcastInstance.getCacheManager();
cache = cacheManager.getCache("my_buckets");

// Bucket will be stored in the imap by this ID 
Object bucketId = "666";

// construct bucket
Bucket bucket = Bucket4j.jCacheBuilder(RecoveryStrategy.RECONSTRUCT)
                .withLimitedBandwidth(1_000, Duration.ofMinutes(1))
                .withLimitedBandwidth(200, Duration.ofSeconds(10))
                .build(cache, KEY);
```

#### Example of Apache Ignite(GridGain) integration 

``` java
Ignite ignite = Ignition.start();
...

// You can use spring configuration if do not want to configure cache in the java code  
CacheConfiguration cfg = new CacheConfiguration("my_buckets");

// setup cache configuration as you wish
cfg.setXXX...
cache = ignite.getOrCreateCache(cfg);

// Bucket will be stored in the Ignite cache by this ID 
Object bucketId = "21";

// construct bucket
Bucket bucket = Bucket4j.jCacheBuilder(RecoveryStrategy.RECONSTRUCT)
                .withLimitedBandwidth(1_000, Duration.ofMinutes(1))
                .withLimitedBandwidth(200, Duration.ofSeconds(10))
                .build(cache, KEY);
```


Have a question?
----------------
Feel free to ask in the [gitter chat](https://gitter.im/vladimir-bukhtoyarov/bucket4j) 

License
-------
Copyright 2015-2017 Vladimir Bukhtoyarov
Licensed under the Apache Software License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.

