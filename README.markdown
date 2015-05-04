# Warning! Project is under development

Bucket4j - is a java implementation of token/leaky bucket algorithm for rate limiting
=====================================================================================
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
* Effective lock free implementation without any critical section, Bucket4j is good scalable for multithreading environment.
* Rich API:
  * More then one bandwidth per bucket. For example you can limit 1000 events per hours but no often then 100 events per minute. 
  * Customizable time measurement. You are able to specify how to time will be measurement: as `System.nanotime()` or `System.currentTimeMillis()`
or you can to specify own way to measure time.
  * Ability to have guaranteed bandwidth. Think about guaranteed bandwidth like a 911 number which you can to call when no money on your balance.
  * Dynamic capacity. If needs capacity of the bucket can be changed over time.
* Ability to switch from one JVM to cluster in one line of code, so using Bucket4j you are able to limiting something in the JVM cluster.
At the moment following grids are supported:
  * [Oracle Coherence](http://www.oracle.com/technetwork/middleware/coherence/overview/index-087514.html) - One of the oldest and most reliable commercial In-Memory Grid. 
  * [Hazelcast](http://hazelcast.com/products/hazelcast/) - The most popular Open Source In-Memory Data Grid.
  * [Apache Ignite(GridGain in the past)](http://www.oracle.com/technetwork/middleware/coherence/overview/index-087514.html) - The Open Source In-Memory Data Grid with most richest API in the world.

### Usage

#### Maven Setup

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
    <version>1.0.0</version>
</dependency>
```

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
                .withLimitedBandwidth(10, TimeUnit.SECONDS.toNanos(1))
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
                .withLimitedBandwidth(100, TimeUnit.MINUTES.toNanos(1))
                .build();

// ...
while (true) {
  // Consume a token from the token bucket.  If a token is not available this method will block until
  // the refill adds one to the bucket.
  bucket.consume(1);

  poll();
}
```

#### Example of multiple bandwidth

Imagine that you are developing load testing tool, in order to be ensure that testable system is able to dispatch 1000 requests per 1 minute.
But you do not want to randomly kill the testable system by generation all 1000 events in one second instead of 1 minute. 
To solve problem you can construct following bucket:
```java
Bucket bucket = Buckets.withNanoTimePrecision()
       // allows 1000 tokens per 1 minute
       .withLimitedBandwidth(1000, TimeUnit.MINUTES.toNanos(1))
       // but not often then 50 tokens per 1 second
       .withLimitedBandwidth(50, TimeUnit.SECOND.toNanos(1))
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
... **TBD**

#### Example of Oracle Coherence integration 
... **TBD**

#### Example of Hazelcast integration 
... **TBD**

#### Example of Apache Ignite(GridGain) integration 
... **TBD**

### Advanced usages

#### Using dynamic capacity  
... **TBD**

#### Using initial capacity  
... **TBD**

#### Using custom time metter  
... **TBD**

License
-------
Copyright 2015 Vladimir Bukhtoyarov
Licensed under the Apache Software License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.