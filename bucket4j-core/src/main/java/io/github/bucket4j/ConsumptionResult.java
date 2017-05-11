package io.github.bucket4j;

/**
 * Describes both result of consumption and tokens remaining in the bucket after consumption.
 *
 * @see Bucket#tryConsumeAndReturnRemainingTokens(long)
 * @see Bucket#tryConsumeAsMuchAsPossibleAndReturnRemainingTokens(long)
 */
public class ConsumptionResult {

    private final boolean consumed;
    private final long remainingTokens;

    public ConsumptionResult(boolean consumed, long remainingTokens) {
        this.consumed = consumed;
        this.remainingTokens = remainingTokens;
    }

    /**
     * Flag describes result of consumption operation.
     *
     * @return true if tokens was consumed
     */
    public boolean isConsumed() {
        return consumed;
    }

    /**
     * Return the tokens remaining in the bucket
     *
     * @return the tokens remaining in the bucket
     */
    public long getRemainingTokens() {
        return remainingTokens;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConsumptionResult{");
        sb.append("consumed=").append(consumed);
        sb.append(", remainingTokens=").append(remainingTokens);
        sb.append('}');
        return sb.toString();
    }

}
