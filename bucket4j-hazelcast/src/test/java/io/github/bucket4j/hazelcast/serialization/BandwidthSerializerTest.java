package io.github.bucket4j.hazelcast.serialization;

import com.hazelcast.nio.serialization.StreamSerializer;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.grid.hazelcast.serialization.HazelcastSerializer;
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
        return HazelcastSerializer.BANDWIDTH_SERIALIZER;
    }

    @Override
    protected void runAssertions(Bandwidth original, Bandwidth deserialized) {
        SerializationAssertions.assertEquals(original, deserialized);
    }



}
