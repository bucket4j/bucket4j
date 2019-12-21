package io.github.bucket4j.hazelcast.serialization;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import org.junit.Assert;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

class SerializationAssertions {

    static void assertEquals(Bandwidth original, Bandwidth deserialized) {
        original.equals(deserialized);
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
        assertTrue(original.equals(deserialized));
    }

    private SerializationAssertions() {
    }
}
