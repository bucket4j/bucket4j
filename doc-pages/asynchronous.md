# Asynchrous API
Since version ```3.0``` Bucket4j provides asynchronous analogs for majority of API methods.
Async view of bucket is availble through ```asAsync()``` method:
```
Bucket bucket = ...;
AsyncBucket asyncBucket = bucket.asAsync();
```
Each method of class [AsyncBucket](https://github.com/vladimir-bukhtoyarov/bucket4j/blob/3.0/bucket4j-core/src/main/java/io/github/bucket4j/AsyncBucket.java)
 has full equvalence with same semantic in synchronous version in the [Bucket](https://github.com/vladimir-bukhtoyarov/bucket4j/blob/3.0/bucket4j-core/src/main/java/io/github/bucket4j/Bucket.java) class.

### Example - limiting the rate of access to asynchronous servlet
Imagine that you develop SMS service, which allows send SMS via HTTP interface.
You want from your architecture to be protected from overloding, clustered and fully asynchronous.

**Overloading protection requirement:**
to prevent fraud and service overloading you want to introduce following limit for any outbound phone number:
> The bucket size is 20 SMS (which cannot be exceeded at any given time), with a "refill rate" of 10 SMS per minute that continually increases tokens in the bucket.
In other words, if client sends 10 SMS per minute, it will never be throttled,
and moreover client have overdraft equals to 20 SMS which can be used if average is little bit higher that 10 SMS/minute on short time period.
**Solution:** lets use bucket4j for this.

**Clustering requirement:**
You want to avoid the single point of failure, if one server crashed that information about consumed tokens should not be lost,
thus it would be better to use any distributed computation platform for storing the buckets.
**Solution:** lets use JBoss Infinispan for this and ```bucket4j-infinispan``` extension.
Hazelcast and Apache Ignite will be also well choice, Infinispan just selected as example.

**Asynchronous processing requirement:**
Also for maximum scalability you want from architecture to be fully non-blocking,
non-blocking architecture means that both SMS sending and limit checking should be asynchronous.
**Solution:** lets use asynchronous features provided by bucket4j and Servlet-API.

**Mockup of service based on top of Servlet API and bucket4j-infinispan**:
```java
import io.github.bucket4j.Bucket4j;

public class ThrottlingFilter implements javax.servlet.Filter {

    // TODO rewrite to servlet

    private Bucket createNewBucket() {
         long overdraft = 20;
         Refill refill = Refill.smooth(10, Duration.ofMinutes(1));
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