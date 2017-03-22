
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
        this.refillTokensPerOneMillis = (double) refillPeriodMillis / (double) refillPeriodMillis;

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
            MinimalisticTokenBucket limiter = new MinimalisticTokenBucket(100, 100,1000);

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
