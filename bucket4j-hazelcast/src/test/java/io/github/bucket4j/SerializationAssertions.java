package io.github.bucket4j;

import org.junit.Assert;

import static org.junit.Assert.assertArrayEquals;

class SerializationAssertions {

    static void assertEquals(Bandwidth original, Bandwidth deserialized) {
        Assert.assertEquals(original.getCapacity(), deserialized.getCapacity());
        Assert.assertEquals(original.getInitialTokens(), deserialized.getInitialTokens());
        Assert.assertEquals(original.getRefillPeriodNanos(), deserialized.getRefillPeriodNanos());
        Assert.assertEquals(original.getRefillTokens(), deserialized.getRefillTokens());
        Assert.assertEquals(original.refillIntervally, deserialized.refillIntervally);
        Assert.assertEquals(original.getTimeOfFirstRefillMillis(), deserialized.getTimeOfFirstRefillMillis());
        Assert.assertEquals(original.useAdaptiveInitialTokens, deserialized.useAdaptiveInitialTokens);
    }

    static void assertEquals(BucketConfiguration original, BucketConfiguration deserialized) {
        Bandwidth[] originalBandwidths = original.getBandwidths();
        Bandwidth[] deserializedBandwidths = deserialized.getBandwidths();
        Assert.assertEquals(originalBandwidths.length, deserializedBandwidths.length);

        for (int ii = 0; ii < deserializedBandwidths.length; ii++) {
            Bandwidth originalBandwidth = originalBandwidths[ii];
            Bandwidth deserializedBandwidth = deserializedBandwidths[ii];
            assertEquals(originalBandwidth, deserializedBandwidth);
        }
    }

    static void assertEquals(BucketState original, BucketState deserialized) {
        assertArrayEquals(original.stateData, deserialized.stateData);
    }

    private SerializationAssertions() {
    }
}
