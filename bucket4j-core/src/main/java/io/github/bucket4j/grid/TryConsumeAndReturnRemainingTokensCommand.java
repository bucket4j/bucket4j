package io.github.bucket4j.grid;

import io.github.bucket4j.ConsumptionProbe;


public class TryConsumeAndReturnRemainingTokensCommand implements GridCommand<ConsumptionProbe> {

    private static final long serialVersionUID = 1L;

    private long tokensToConsume;
    private boolean bucketStateModified = false;

    public TryConsumeAndReturnRemainingTokensCommand(long tokensToConsume) {
        this.tokensToConsume = tokensToConsume;
    }

    @Override
    public ConsumptionProbe execute(GridBucketState state, long currentTimeNanos) {
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToConsume <= availableToConsume) {
            state.consume(tokensToConsume);
            bucketStateModified = true;
            return ConsumptionProbe.consumed(availableToConsume - tokensToConsume);
        } else {
            long nanosToWaitForRefill = state.delayNanosAfterWillBePossibleToConsume(tokensToConsume);
            return ConsumptionProbe.rejected(availableToConsume, nanosToWaitForRefill);
        }
    }

    @Override
    public boolean isBucketStateModified() {
        return bucketStateModified;
    }

}
