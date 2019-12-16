package io.github.bucket4j;

import com.hazelcast.nio.serialization.StreamSerializer;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static io.github.bucket4j.Bandwidth.classic;
import static io.github.bucket4j.Bandwidth.simple;
import static io.github.bucket4j.Refill.greedy;
import static io.github.bucket4j.Refill.intervally;
import static io.github.bucket4j.Refill.intervallyAligned;
import static java.time.Duration.ofSeconds;

public class BandwidthSerializerTest extends SerializerTest<Bandwidth> {

    @Override
    protected StreamSerializer<Bandwidth> getSerializerUnderTest() {
        return this.bandwidthSerializer;
    }

    @Override
    protected void runAssertions(Bandwidth original, Bandwidth deserialized) {
        SerializationAssertions.assertEquals(original, deserialized);
    }

    @Test
    public void serializeSimpleBandwidth() throws IOException {
        Bandwidth bandwidth = simple(10, ofSeconds(20));
        testSerialization(bandwidth);
    }

    @Test
    public void serializeClassicBandwidthWithGreedyRefill() throws IOException {
        Bandwidth bandwidth = classic(20, greedy(100, Duration.ofSeconds(42)));
        testSerialization(bandwidth);
    }

    @Test
    public void serializeClassicBandwidthWithIntervallyRefill() throws IOException {
        Bandwidth bandwidth = classic(30, intervally(200, Duration.ofSeconds(420)));
        testSerialization(bandwidth);
    }

    @Test
    public void serializeClassicBandwidthWithIntervallyAlignedRefill() throws IOException {
        Bandwidth bandwidth = classic(40, intervallyAligned(300, Duration.ofSeconds(4200), Instant.now(), true));
        testSerialization(bandwidth);
    }
}
