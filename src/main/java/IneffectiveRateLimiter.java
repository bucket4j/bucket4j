import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The naive solution for rate limiter which wast CPU to refill tokens in background executor
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
