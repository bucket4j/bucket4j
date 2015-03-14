package com.github.bandwidthlimiter.tokenbucket;

public class TokenBucketExceptions {

    public static IllegalArgumentException nonPositiveCapacity(long capacity) {
        // TODO provide error description
        return new IllegalArgumentException();
    }

    public static IllegalArgumentException nonPositiveInitialCapacity(long initialCapacity) {
        // TODO provide error description
        return new IllegalArgumentException();
    }

    public static IllegalArgumentException initialCapacityGreaterThanMaxCapacity(long initialCapacity, long maxCapasity) {
        // TODO provide error description
        return new IllegalArgumentException();
    }

    public static IllegalArgumentException nonPositiveTokensToConsume(long tokens) {
        // return new IllegalArgumentException("Number of tokens to consumeSingleToken must be positive");
        // TODO provide error description
        return new IllegalArgumentException();
    }

    public static IllegalArgumentException tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(long tokens, long smallestCapacity) {
        // TODO provide error description
        return new IllegalArgumentException();
    }

    public static IllegalArgumentException nullRefillStrategy() {
        // TODO provide error description
        return new IllegalArgumentException();
    }

    public static IllegalArgumentException nullWaitingStrategy() {
        // TODO provide error description
        return new IllegalArgumentException();
    }

    public static IllegalArgumentException nullTimeUnit() {
        // TODO provide error description
        return new IllegalArgumentException();
    }

    public static IllegalArgumentException nonPositivePeriod(long period) {
        // TODO provide error description
        return new IllegalArgumentException();
    }

    public static IllegalArgumentException nonPositiveWaitingNanos(long waitIfBusyNanos) {
        // TODO provide error description
        return new IllegalArgumentException();
    }

    public static IllegalArgumentException restrictionsNotSpecified() {
        // TODO provide error description
        return new IllegalArgumentException();
    }

    public static IllegalArgumentException guarantedHasGreaterRateThanRestricted(BandwidthDefinition guaranteed, BandwidthDefinition restricted) {
        // TODO provide error description
        return new IllegalArgumentException();
    }

}
