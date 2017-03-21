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
public class NaiveRateLimiter {

    private long availableTokens;
    private final long periodMillis;

    private LinkedList<Issue> issuedTokens = new LinkedList<>();

    /**
     * Creates instance of rate limiter which provides guarantee that consumption rate will be >= tokens/periodMillis
     */
    public NaiveRateLimiter(long tokens, long periodMillis) {
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
            NaiveRateLimiter limiter = new NaiveRateLimiter(100, 1000);

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
### Problems in naive solution
The solution above consumes memory in non-effective way, to satisfy the rate limitation, 
this implementation stores each particular fact about issued tokens at least for ```periodMillis```.
And this behavior can lead to following problems:
- JVM can crash with out of memory error in case of rate of events to high or period is too large.
- If JVM can survive, the problem of unnecessary promotion from young to tenured memory region still exists. 
Imagine that minor garbage collections happen each 5 second, and you have deal with 1 minute period of limiter -
It is obviously that memory for each issued permit primary allocated in new generation 
and then will be promoted to tenured generation because time to live of permit is enough to survive several collections in new generation.
Hence this naive implementation will lead to more frequent Full GC pauses.
 
### So, what kinds of problems can be solved by token bucket?
Token bucket algorithm solves rate limiting problems in way which requires small and predefined amount of memory, 
independently of incoming request rate the memory consumed by token-bucket is always constant.

## High-level model of token-bucket algorithm
```TODO```

See for more details:

* [Wikipedia - Token Bucket](http://en.wikipedia.org/wiki/Token_bucket)
* TODO