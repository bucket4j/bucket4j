package com.github.bandwidthlimiter.leakybucket;

public final class TokenBucketRefillStrategy implements RefillStrategy {

    private final long period;
    private final long tokens;

    public TokenBucketRefillStrategy(long period, long tokens) {
        // TODO args validation
        this.period = period;
        this.tokens = tokens;
    }

    @Override
    public void setupInitialState(LeakyBucketConfiguration configuration, LeakyBucketState state, long currentTime) {
        for (Bandwidth bandwidth: configuration.getBandwidths()) {
            state.setCurrentSize(bandwidth, bandwidth.getInitialCapacity());
            state.setRefillState(configuration, bandwidth.getIndexInBucket(), currentTime);
        }
    }

    @Override
    public void refill(LeakyBucketConfiguration configuration, LeakyBucketState state, long currentTime) {
        for (Bandwidth bandwidth: configuration.getBandwidths()) {
            final int i = bandwidth.getIndexInBucket();
            long previousRefillTime = state.getRefillState(configuration, i);
            final long maxCapacity = bandwidth.getMaxCapacity();

            long durationSinceLastRefill = currentTime - previousRefillTime;
            if (durationSinceLastRefill < this.period) {
                continue;
            }

            final long bandwidthPeriod = bandwidth.getPeriod();
            if (durationSinceLastRefill > bandwidthPeriod) {
                state.setCurrentSize(bandwidth, maxCapacity);
                state.setRefillState(configuration, i, currentTime);
                continue;
            }

            long periodCount = durationSinceLastRefill / this.period;
            if (periodCount == 0) {
                continue;
            }

            long calculatedRefill = tokens * periodCount;
            long newSize = state.getCurrentSize(bandwidth) + calculatedRefill;
            if (newSize >= maxCapacity) {
                state.setCurrentSize(bandwidth, maxCapacity);
                state.setRefillState(configuration, i, currentTime);
                continue;
            }

            state.setCurrentSize(bandwidth, newSize);
            long durationCorrection = durationSinceLastRefill % this.period;
            long effectiveRefillTime = currentTime - durationCorrection;
            state.setRefillState(configuration, i, effectiveRefillTime);
        }
    }

    @Override
    public long timeRequiredToRefill(LeakyBucketConfiguration configuration, Bandwidth bandwidth, long numTokens) {
        return bandwidth.getPeriod() * numTokens / bandwidth.getMaxCapacity();
    }

    @Override
    public int sizeOfState(LeakyBucketConfiguration configuration) {
        return configuration.getBandwidthCount();
    }

    @Override
    public String toString() {
        return "TokenBucketRefillStrategy{" +
                "period=" + period +
                ", tokens=" + tokens +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TokenBucketRefillStrategy that = (TokenBucketRefillStrategy) o;

        if (period != that.period) return false;
        return tokens == that.tokens;

    }

    @Override
    public int hashCode() {
        int result = (int) (period ^ (period >>> 32));
        result = 31 * result + (int) (tokens ^ (tokens >>> 32));
        return result;
    }

}
