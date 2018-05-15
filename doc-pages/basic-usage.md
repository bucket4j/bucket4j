# Basic usage examples

### Example 1 - limiting the rate of heavy work
Imagine that you have a thread-pool executor and you want to know what your threads are doing in the moment when thread-pool throws RejectedExecutionException.
Printing stacktraces of all threads in the JVM will be the best way to know where are all threads have stuck and why thread-pool is overflown.
But acquiring stacktraces is very cost operation by itself, and you want to do it not often than 1 time per 10 minutes:
```java
// define the limit 1 time per 10 minute
Bandwidth limit = Bandwidth.simple(1, Duration.ofMinutes(10));
// construct the bucket
Bucket bucket = Bucket4j.builder().addLimit(limit).build();

...

try {
   executor.execute(anyRunnable);
} catch (RejectedExecutionException e) {
    // print stacktraces only if limit is not exceeded
    if (bucket.tryConsume(1)) {
        ThreadInfo[] stackTraces = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
        StacktraceUtils.print(stackTraces);
    }
    throw e;
}

```

### Example 2 - using bucket as scheduler
Suppose you need to have the fresh exchange rate between dollars and euros. 
To get the rate you continuously poll the third-party provider, 
and by contract with provider you should poll not often than 100 times per 1 minute, else provider will block your ip:   
```java
// define the limit 100 times per 1 minute
Bandwidth limit = Bandwidth.simple(100, Duration.ofMinutes(1));
// construct the bucket
Bucket bucket = Bucket4j.builder().addLimit(limit).build();

...
volatile double exchangeRate;
...

// do polling in infinite loop
while (true) {
  // Consume a token from the token bucket.  
  // If a token is not available this method will block until the refill adds one to the bucket.
  bucket.asScheduler().consume(1);

  exchangeRate = pollExchangeRate();
}
```

### Example 3 - limiting the rate of access to REST API
Imagine that you develop yet another social network and you want to provide REST API for third-party developers.
To protect your system from overloading you want to introduce following limitation:

> The bucket size is 50 calls (which cannot be exceeded at any given time), with a "refill rate" of 10 calls per second that continually increases tokens in the bucket. 
In other words. if client app averages 10 calls per second, it will never be throttled, 
and moreover client have overdraft equals to 50 calls which can be used if average is little bit higher that 10 call/sec on short time period.

Constructing the bucket to satisfy the requirements above is little bit more complicated that for previous examples, 
because we have deal with overdraft, but it is not rocket science:
```java
import io.github.bucket4j.Bucket4j;

public class ThrottlingFilter implements javax.servlet.Filter {

    private Bucket createNewBucket() {
         long overdraft = 50; 
         Refill refill = Refill.greedy(10, Duration.ofSeconds(1));
         Bandwidth limit = Bandwidth.classic(overdraft, refill);
         return Bucket4j.builder().addLimit(limit).build();
    }
    
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpSession session = httpRequest.getSession(true);

        String appKey = SecurityUtils.getThirdPartyAppKey();
        Bucket bucket = (Bucket) session.getAttribute("throttler-" + appKey);
        if (bucket == null) {
            Bucket bucket = createNewBucket();
            session.setAttribute("throttler-" + appKey, bucket);
        }

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
```
If you want provide more information to end user about the state of bucket, then last fragment of code above can be rewritten in following way:
```java
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            // the limit is not exceeded
            httpResponse.setHeader("X-Rate-Limit-Remaining", "" + probe.getRemainingTokens());
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            // limit is exceeded
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setStatus(429);
            httpResponse.setHeader("X-Rate-Limit-Retry-After-Seconds", "" + TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
            httpResponse.setContentType("text/plain");
            httpResponse.getWriter().append("Too many requests");
        }
```
