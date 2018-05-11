# Asynchrous API
Since version ```3.0``` Bucket4j provides asynchronous analogs for majority of API methods.
Async view of bucket is availble through ```asAsync()``` method:
```
Bucket bucket = ...;
AsyncBucket asyncBucket = bucket.asAsync();
```
Each method of class [AsyncBucket](https://github.com/vladimir-bukhtoyarov/bucket4j/blob/3.1/bucket4j-core/src/main/java/io/github/bucket4j/AsyncBucket.java)
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

public class SmsServlet extends javax.servlet.http.HttpServlet {

    private SmsSender smsSender;
    private ProxyManager<String> buckets;
    private Supplier<BucketConfiguration> configuration;
       
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        
        smsSender = (SmsSender) ctx.getAttribute("sms-sender");
        
        FunctionalMapImpl<String, GridBucketState> bucketMap = (FunctionalMapImpl<String, GridBucketState>) ctx.getAttribute("bucket-map")
        this.buckets = Bucket4j.extension(Infinispan.class).proxyManagerForMap(bucketMap);
        
        this.configuration = () -> {
            long overdraft = 20;
            Refill refill = Refill.greedy(10, Duration.ofMinutes(1));
            Bandwidth limit = Bandwidth.classic(overdraft, refill);
            return Bucket4j.configurationBuilder()
                .addLimit(limit)
                .build();
        };
    }
    
    @Override
    protected void doPost(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        
        String fromNumber = req.getParameter("from");
        String toNumber = req.getParameter("to");
        String text = req.getParameter("text");
        
        Bucket bucket = buckets.getProxy(fromNumber, configuration);
        CompletableFuture<ConsumptionProbe> limitCheckingFuture = bucket.asAsync().tryConsumeAndReturnRemaining(1);
        final AsyncContext asyncContext = req.startAsync();
        limitCheckingFuture.thenCompose(probe -> {
            if (!probe.isConsumed()) {
                Result throttledResult = Result.throttled(probe);
                return CompletableFuture.completedFuture(throttledResult);
            } else {
                CompletableFuture<Result> sendingFuture = smsSender.sendAsync(fromNumber, toNumber, text);
                return sendingFuture;
            }
        }).whenComplete((result, exception) -> {
            HttpServletResponse asyncResponse = (HttpServletResponse) asyncContext.getResponse();
            try {
                asyncResponse.setContentType("text/plain");
                if (exception != null || result.isFailed()) {
                    asyncResponse.setStatus(500);
                    asyncResponse.getWriter().println("Internal Error");
                } else if (result.isThrottled()) {
                    asyncResponse.setStatus(429);
                    asyncResponse.setHeader("X-Rate-Limit-Retry-After-Seconds", "" + result.getRetryAfter());
                    asyncResponse.getWriter().append("Too many requests");
                } else {
                    asyncResponse.setStatus(200);
                    asyncResponse.getWriter().append("Success");
                }
            } finally{
                asyncContext.complete();
            }
        });
    }

}
```
