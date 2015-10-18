## Bucket4j - is a java implementation of token/leaky bucket algorithm for rate limiting
[![Build Status](https://travis-ci.org/vladimir-bukhtoyarov/bucket4j.svg?branch=master)](https://travis-ci.org/vladimir-bukhtoyarov/bucket4j)
[![Coverage Status](https://coveralls.io/repos/vladimir-bukhtoyarov/bucket4j/badge.svg)](https://coveralls.io/r/vladimir-bukhtoyarov/bucket4j)

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
* Absolute precision, Bucket4j does not operate with floats or doubles, all calculation are performed in the integer arithmetic.
* Rich API:
  * More then one bandwidth per bucket. For example you can limit 1000 events per hours but not often then 100 events per minute. 
  * Customizable time measurement. You are able to specify how to time will be measurement: as `System.nanotime()` or `System.currentTimeMillis()`
or you can to specify own way to measure time.
  * Ability to have guaranteed bandwidth. Think about guaranteed bandwidth like a 911 number which you can to call when no money on your balance.
  * Dynamic capacity. If needs capacity of the bucket can be changed over time.
* Ability to switch from one JVM to cluster in one line of code, so using Bucket4j you are able to limiting something in the JVM cluster.
At the moment following grids are supported:
  * [Oracle Coherence](http://www.oracle.com/technetwork/middleware/coherence/overview/index-087514.html) - One of the oldest and most reliable commercial In-Memory Grid. 
  * [Hazelcast](http://hazelcast.com/products/hazelcast/) - The most popular Open Source In-Memory Data Grid.
  * [Apache Ignite(GridGain in the past)](http://www.oracle.com/technetwork/middleware/coherence/overview/index-087514.html) - Yet another open source In-Memory Data Grid.

### Get Bucket4j library

#### By direct link
[Download compiled jar, sources, javadocs](https://github.com/vladimir-bukhtoyarov/bucket4j/releases/tag/1.0.1)

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
    <version>1.0.1</version>
</dependency>
```

### Basic usage

#### Simple example

Imagine that you develop WEB application and want to limit user to access for application no often then 10 times for second:

```java
public class ThrottlingFilter implements javax.servlet.Filter {
    
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpSession session = httpRequest.getSession(true);

        Bucket bucket = (Bucket) session.getAttribute("throttler");
        if (bucket == null) {
            // build bucket with required capacity and associate it with particular user
            bucket = Buckets.withNanoTimePrecision()
                .withLimitedBandwidth(10, TimeUnit.SECONDS, 1)
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

// Create a token bucket with required capacity.
Bucket bucket = Buckets.withNanoTimePrecision()
                .withLimitedBandwidth(100, TimeUnit.MINUTES, 1)
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
Bucket bucket = Buckets.withNanoTimePrecision()
       // allows 1000 tokens per 1 minute
       .withLimitedBandwidth(1000, TimeUnit.MINUTES, 1)
       // but not often then 50 tokens per 1 second
       .withLimitedBandwidth(50, TimeUnit.SECOND, 1)
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

Bucket bucket = Buckets.withNanoTimePrecision()
    .withLimitedBandwidth(1000, TimeUnit.HOURS, 1)
    .withGuaranteedBandwidth(1, TimeUnit.MINUTES, 10)
    .build();
```

#### Using initial capacity  

By default initial size of bucket is equal to capacity. 
But sometimes, you may want to have lesser initial size, for example for case of cold start in order to prevent denial of service. 
You can specify initial size as third parameter during bandwidth construction:

```java

int initialCapacity = 42;

Bucket bucket = Buckets.withNanoTimePrecision()
    .withLimitedBandwidth(1000, TimeUnit.HOURS, 1, initialCapacity)
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
Buckets.withMillisTimePrecision()
    .withLimitedBandwidth(adjuster, TimeUnit.MINUTES, 1, 10);
```

#### Customizing time measurement 
You can specify your custom time meter, if existing nanotime or miliseconds time meters is not enough for your purposes. For example:

```java

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class CurrentThreadCpuTimeMeter implements TimeMeter {

    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

    @Override
    public long currentTime() {
        return THREAD_MX_BEAN.getCurrentThreadCpuTime();
    }

    @Override
    public void sleep(long units) throws InterruptedException {
        LockSupport.parkNanos(units);
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    @Override
    public long toBandwidthPeriod(TimeUnit timeUnit, long period) {
        return timeUnit.toNanos(period);
    }

}

Bucket bucket = Buckets.withCustomTimePrecision(new CurrentThreadCpuTimeMeter())
                .withLimitedBandwidth(100, TimeUnit.MINUTES, 1)
                .build();


```

### Examples of distributed usage 

#### Example of Oracle Coherence integration 

``` java
// Cache with name "my_buckets" should be configured as described in the documentation for Oracle Coherence.  
NamedCache cache = com.tangosol.net.CacheFactory.getCache("my_buckets");

// Bucket will be stored in the cache by this ID 
Object bucketId = "42";

// construct bucket
Bucket bucket = Buckets.withMillisTimePrecision()
                .withLimitedBandwidth(100, TimeUnit.MINUTES, 1)
                .buildCoherence(cache, bucketId);
```

#### Example of Hazelcast integration 
``` java
  
HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

// In opposite to Coherence, Hazelcast does not require to configure cache before using it,
// but it would be better to configure map "my_buckets" as described in Hazelcast documentation,
// because Hazelcast enables backups by default, and backup feature leads to performance degradation. 
IMap<Object, GridBucketState> imap = hazelcastInstance.getMap("my_buckets");

// Bucket will be stored in the imap by this ID 
Object bucketId = "666";

// construct bucket
Bucket bucket = Buckets.withMillisTimePrecision()
                .withLimitedBandwidth(100, TimeUnit.MINUTES, 1)
                .buildHazelcast(imap, bucketId);
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
Bucket bucket = Buckets.withMillisTimePrecision()
                .withLimitedBandwidth(100, TimeUnit.MINUTES, 1)
                .buildIgnite(cache, bucketId);
```

License
-------
Copyright 2015 Vladimir Bukhtoyarov
Licensed under the Apache Software License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.