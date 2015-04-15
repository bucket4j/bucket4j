package com.github.bandwidthlimiter.bucket;

import java.text.MessageFormat;

public class BucketExceptions {

    private BucketExceptions() {}

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

    public static IllegalArgumentException nullBandwidthAdjuster() {
        String msg = "Bandwidth adjuster can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullTimeMetter() {
        String msg = "Time metter can not be null";
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

    public static IllegalArgumentException guarantedHasGreaterRateThanLimited(BandwidthDefinition guaranteed, BandwidthDefinition limited) {
        String pattern = "Misconfiguration detected, guaranteed bandwidth {0} has higher rate than limited bandwidth {1}";
        String msg = MessageFormat.format(pattern, guaranteed, limited);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException hasOverlaps(BandwidthDefinition first, BandwidthDefinition second) {
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

}
