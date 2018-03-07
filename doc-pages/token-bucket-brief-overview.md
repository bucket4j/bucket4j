# Brief overview of token-bucket algorithm
## What kinds of problems can be solved by token bucket?
Token-bucket is solution for rate limiting. Sometimes to understand solution it is better to start from understanding the problem.
Lets implement a simple algorithm for limitation defined in terms ```N``` events per ```M``` **rolling** time window.
#### Naive rate limiting solution
```java
import java.util.LinkedList;

/**
 * The naive solution for rate limiter which potentially leads to crash JVM with out of memory error.
 */
public class MinimalisticTokenBucket {

    private long availableTokens;
    private final long periodMillis;

    private LinkedList<Issue> issuedTokens = new LinkedList<>();

    /**
     * Creates instance of rate limiter which provides guarantee that consumption rate will be >= tokens/periodMillis
     */
    public MinimalisticTokenBucket(long tokens, long periodMillis) {
        this.availableTokens = tokens;
        this.periodMillis = periodMillis;
    }

    synchronized public boolean tryConsume(int numberTokens) {
        long nowMillis = System.currentTimeMillis();
        clearObsoleteIssues(nowMillis);

        if (availableTokens < numberTokens) {
            // has no requested tokens in the bucket
            return false;
        } else {
            issuedTokens.addLast(new Issue(numberTokens, nowMillis));
            availableTokens -= numberTokens;
            return true;
        }
    }

    private void clearObsoleteIssues(long nowMillis) {
        while (!issuedTokens.isEmpty()) {
            Issue issue = issuedTokens.getFirst();
            if (nowMillis - issue.timestampMillis > periodMillis) {
                availableTokens += issue.tokens;
                issuedTokens.removeFirst();
            } else {
                return;
            }
        }
    }

    private static final class Issue {
        private final long tokens;
        private final long timestampMillis;

        private Issue(long tokens, long timestampMillis) {
            this.tokens = tokens;
            this.timestampMillis = timestampMillis;
        }
    }

    private static final class Selftest {

        public static void main(String[] args) {
            // 100 tokens per 1 second
            MinimalisticTokenBucket limiter = new MinimalisticTokenBucket(100, 1000);

            long startMillis = System.currentTimeMillis();
            long consumed = 0;
            while (System.currentTimeMillis() - startMillis < 10000) {
                if (limiter.tryConsume(1)) {
                    consumed++;
                }
            }
            System.out.println(consumed);
        }

    }

}
```
**What are the problems with this naive implementation?**
The solution above consumes memory in non-effective way, in order to control rate, it stores each particular fact about issued tokens at least for ```periodMillis```.
And this behavior can lead to following problems:
- JVM can crash with out of memory error in case of rate of events to high or period is too large.
- If JVM can survive, the problem of unnecessary promotion from young to tenured memory region still exists. 
Imagine that minor garbage collections happen each 5 second, and you have deal with 1 minute period of limiter -
It is obviously that memory for each issued permit primary allocated in new generation 
and then will be promoted to tenured generation because time to live of permit is enough to survive several collections in new generation.
Hence this naive implementation will lead to more frequent Full GC pauses.

#### Attempt to optimize memory consumption
Ok, previous attempt was fail, but I can optimize memory consumption by refilling available tokens in background thread instead of storing each fact about consumption.
Lets do it: 
```java
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The ineffective solution for rate limiter which wast CPU to refill tokens in background executor
 */
public class MinimalisticTokenBucket {

    private final long maxTokens;
    private long availableTokens;

    /**
     * Creates instance of rate limiter which provides guarantee that consumption rate will be >= tokens/periodMillis
     */
    public MinimalisticTokenBucket(long tokens, long periodMillis, ScheduledExecutorService scheduler) {
        long millisToRefillOneToken = periodMillis / tokens;
        scheduler.scheduleAtFixedRate(this::addToken, periodMillis, millisToRefillOneToken, TimeUnit.MILLISECONDS);

        this.maxTokens = tokens;
        this.availableTokens = tokens;
    }

    synchronized private void addToken() {
        availableTokens = Math.min(maxTokens, availableTokens + 1);
    }

    synchronized public boolean tryConsume(int numberTokens) {
        if (availableTokens < numberTokens) {
            return false;
        } else {
            availableTokens -= numberTokens;
            return true;
        }
    }

    private static final class Selftest {

        public static void main(String[] args) {
            // 100 tokens per 1 second
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            MinimalisticTokenBucket limiter = new MinimalisticTokenBucket(100, 1000, scheduler);

            long startMillis = System.currentTimeMillis();
            long consumed = 0;
            while (System.currentTimeMillis() - startMillis < 10000) {
                if (limiter.tryConsume(1)) {
                    consumed++;
                }
            }
            scheduler.shutdown();
            System.out.println(consumed);
        }

    }

}

```
**What are the problems?** This implementation is not self-sufficient because requires ScheduledExecutorService, and this leads to following disadvantages:  
- Requires at least one background thread.
- CPU can be heavy consumed in case of multiple limiters or high granular refill. 
For example, limit rate 100 events per 1 second, requires 100 executions per second inside scheduler, for case with 100 independent limiters it is need to 10000 executions per second.
- CPU is used for scheduling background tasks even if limiter was unused for a long time.
- Requires tricky management of memory and tasks: it is need to cancel task in scheduler when limiter is not needed anymore, also references(task in scheduler holds strong reference to limiter which makes limiter reachable).
 
#### So, what kinds of problems can be solved by token bucket?
Token bucket algorithm solves rate limiting problems in way:
- Which requires small and predefined amount of memory, independently of incoming request rate the memory consumed by token-bucket is always constant.
- There are no any additional background threads required by token-bucket. 

## Token-bucket algorithm
#### Overview of token-bucket 
The token bucket algorithm is based on an analogy of a fixed capacity bucket into which tokens, normally representing a unit of bytes or a single packet of predetermined size, are added at a fixed rate. 
When a packet is to be checked for conformance to the defined limits, the bucket is inspected to see if it contains sufficient tokens at that time. 
If so, the appropriate number of tokens, e.g. equivalent to the length of the packet in bytes, are removed ("cashed in"), and the packet is passed, e.g., for transmission.
The packet does not conform if there are insufficient tokens in the bucket, and the contents of the bucket are not changed. Non-conformant packets can be treated in various ways:
- They may be dropped.
- They may be enqueued for subsequent transmission when sufficient tokens have accumulated in the bucket.
- They may be transmitted, but marked as being non-conformant, possibly to be dropped subsequently if the network is overloaded.

A conforming flow can thus contain traffic with an average rate up to the rate at which tokens are added to the bucket, and have a burstiness determined by the depth of the bucket. This burstiness may be expressed in terms of either a jitter tolerance, i.e. how much sooner a packet might conform (e.g. arrive or be transmitted) than would be expected from the limit on the average rate, or a burst tolerance or maximum burst size, i.e. how much more than the average level of traffic might conform in some finite period.

#### Formal model of token-bucket 
The token bucket model can be conceptually understood as follows:
- A token is added to the bucket every ```1/r``` seconds.
- The bucket can hold at the most ```b``` tokens. If a token arrives when the bucket is full, it is discarded.
- When a packet (network layer PDU) of n bytes arrives, ```n``` tokens are removed from the bucket, and the packet is sent to the network.
- If fewer than ```n``` tokens are available, no tokens are removed from the bucket, and the packet is considered to be non-conformant.

**NOTE:** pay attention that description above is just a formal model of token-bucket, it is not the algorithm by itself.
Concrete algorithms of token-bucket implementation can significantly differ form each other by details, 
but any implementation which called itself as "token-bucket" must produce results in way which can be checked by this model.

#### Example of basic java token-bucket implementation
Lets create the minimalistic implementation of token bucket in java
```java
/**
 * The minimalistic token-bucket implementation
 */
public class MinimalisticTokenBucket {

    private final long capacity;
    private final double refillTokensPerOneMillis;

    private double availableTokens;
    private long lastRefillTimestamp;

    /**
     * Creates token-bucket with specified capacity and refill rate equals to refillTokens/refillPeriodMillis
     */
    public MinimalisticTokenBucket(long capacity, long refillTokens, long refillPeriodMillis) {
        this.capacity = capacity;
        this.refillTokensPerOneMillis = (double) refillTokens / (double) refillPeriodMillis;

        this.availableTokens = capacity;
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

    synchronized public boolean tryConsume(int numberTokens) {
        refill();
        if (availableTokens < numberTokens) {
            return false;
        } else {
            availableTokens -= numberTokens;
            return true;
        }
    }

    private void refill() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis > lastRefillTimestamp) {
            long millisSinceLastRefill = currentTimeMillis - lastRefillTimestamp;
            double refill = millisSinceLastRefill * refillTokensPerOneMillis;
            this.availableTokens = Math.min(capacity, availableTokens + refill);
            this.lastRefillTimestamp = currentTimeMillis;
        }
    }

    private static final class Selftest {

        public static void main(String[] args) {
            // 100 tokens per 1 second
            MinimalisticTokenBucket limiter = new MinimalisticTokenBucket(100, 100, 1000);

            long startMillis = System.currentTimeMillis();
            long consumed = 0;
            while (System.currentTimeMillis() - startMillis < 10000) {
                if (limiter.tryConsume(1)) {
                    consumed++;
                }
            }
            System.out.println(consumed);
        }

    }

}
```
The ```MinimalisticTokenBucket``` is just a learning example which helps to understand token-bucket. please do not make assumptions about how to ```Bucket4j``` internally works looking at ```MinimalisticTokenBucket```,
because ```Bucket4j``` has nothing common with ```MinimalisticTokenBucket```. 
