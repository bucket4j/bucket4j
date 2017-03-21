# Brief overview of token-bucket algorithm
## What kinds of problems can be solved by token bucket?
Token-bucket is solution for rate limiting. Sometimes to understand solution it is better to start from understanding the problem.
Lets implement a simple algorithm for limitation defined in terms ```N``` events per ```M``` **rolling** time window.
### Naive rate limiting solution
```java
import java.util.LinkedList;

/**
 * The naive solution for rate limiter which potentially leads to crash JVM with out of memory error.
 */
public class IneffectiveRateLimiter {

    private long availableTokens;
    private final long periodMillis;

    private LinkedList<Issue> issuedTokens = new LinkedList<>();

    /**
     * Creates instance of rate limiter which provides guarantee that consumption rate will be >= tokens/periodMillis
     */
    public IneffectiveRateLimiter(long tokens, long periodMillis) {
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
            IneffectiveRateLimiter limiter = new IneffectiveRateLimiter(100, 1000);

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

### Ineffective attempt to optimize memory consumption
Ok, previous attempt was fail, but I can optimize memory consumption by refilling available tokens in background thread instead of storing each fact about consumption.
Lets do it: 
```java
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The ineffective solution for rate limiter which wast CPU to refill tokens in background executor
 */
public class IneffectiveRateLimiter {

    private final long maxTokens;
    private long availableTokens;

    /**
     * Creates instance of rate limiter which provides guarantee that consumption rate will be >= tokens/periodMillis
     */
    public IneffectiveRateLimiter(long tokens, long periodMillis, ScheduledExecutorService scheduler) {
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
            IneffectiveRateLimiter limiter = new IneffectiveRateLimiter(100, 1000, scheduler);

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
**What are the problems with this naive implementation?**
- TODO 
- TODO
- TODO
 
### So, what kinds of problems can be solved by token bucket?
Token bucket algorithm solves rate limiting problems in way which requires small and predefined amount of memory, 
independently of incoming request rate the memory consumed by token-bucket is always constant.

## High-level model of token-bucket algorithm
```TODO```

See for more details:

* [Wikipedia - Token Bucket](http://en.wikipedia.org/wiki/Token_bucket)
* TODO