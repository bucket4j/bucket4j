package com.github.bandwidthlimiter.leakybucket;

import java.text.MessageFormat;

public class LeakyBucketExceptions {

    // ------------------- construction time exceptions --------------------------------
    public static IllegalArgumentException nonPositiveCapacity(long capacity) {
        String pattern = "{0} is wrong value for capacity, because capacity should be positive";
        String msg = MessageFormat.format(pattern, capacity);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositiveInitialCapacity(long initialCapacity) {
        String pattern = "{0} is wrong value for initial capacity, because initial capacity should be positive";
        String msg = MessageFormat.format(pattern, initialCapacity);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException initialCapacityGreaterThanMaxCapacity(long initialCapacity, long maxCapasity) {
        String pattern = "initial capacity {0} is greater than max capacity {1}";
        String msg = MessageFormat.format(pattern, initialCapacity, maxCapasity);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullRefillStrategy() {
        String msg = "Refill strategy can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullWaitingStrategy() {
        String msg = "Waiting strategy can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullNanoTimeWrapper() {
        String msg = "Nanotime wrapper can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullTimeUnit() {
        String msg = "Time unit of bandwidth can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositivePeriod(long period) {
        String pattern = "{0} is wrong value for period of bandwidth, because period should be positive";
        String msg = MessageFormat.format(pattern, period);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException restrictionsNotSpecified() {
        String msg = "At list one limited bandwidth should be specified";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException onlyOneGuarantedBandwidthSupported() {
        String msg = "Only one guaranteed bandwidth supported";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException guarantedHasGreaterRateThanLimited(Bandwidth guaranteed, Bandwidth limited) {
        String pattern = "Misconfiguration detected, guaranteed bandwidth {0} has higher rate than limited bandwidth {1}";
        String msg = MessageFormat.format(pattern, guaranteed, limited);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException hasOverlaps(Bandwidth first, Bandwidth second) {
        String pattern = "Overlap detected between {0} and {1}";
        String msg = MessageFormat.format(pattern, first, second);
        return new IllegalArgumentException(msg);
    }
    // ------------------- end of construction time exceptions --------------------------------

    // ------------------- usage time exceptions  ---------------------------------------------
    public static IllegalArgumentException nonPositiveNanosToWait(long waitIfBusyNanos) {
        String pattern = "Waiting value should be positive, {0} is wrong waiting period";
        String msg = MessageFormat.format(pattern, waitIfBusyNanos);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositiveTokensToConsume(long tokens) {
        String pattern = "Unable to consume {0} tokens, due to number of tokens to consume should be positive";
        String msg = MessageFormat.format(pattern, tokens);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(long tokens, long smallestCapacity) {
        String pattern = "Unable to consume {0} tokens, due to it is grater than {1} - capacity of smaller bandwidth";
        String msg = MessageFormat.format(pattern, tokens, smallestCapacity);
        return new IllegalArgumentException(msg);
    }

}
