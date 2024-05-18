package io.github.bucket4j.local;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.util.ComparableByContent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class LocalBucketsSerializationTest {

    @Test
    public void testBinarySerialization() throws IOException {
        for (SynchronizationStrategy strategy : SynchronizationStrategy.values()) {
            LocalBucket sourceBucket = Bucket.builder()
                    .addLimit(Bandwidth.builder().capacity(1).refillGreedy(1, Duration.ofSeconds(1)).build())
                    .withSynchronizationStrategy(strategy)
                    .build();
            byte[] snapshot = sourceBucket.toBinarySnapshot();

            LocalBucket deserializedBucket = LocalBucket.fromBinarySnapshot(snapshot);
            assertTrue(ComparableByContent.equals(sourceBucket, deserializedBucket));
        }
    }

    @Test
    public void testJsonCompatibleSerialization() throws IOException {
        for (SynchronizationStrategy strategy : SynchronizationStrategy.values()) {
            LocalBucket sourceBucket = Bucket.builder()
                    .addLimit(Bandwidth.builder().capacity(1).refillGreedy(1, Duration.ofSeconds(1)).build())
                    .withSynchronizationStrategy(strategy)
                    .build();
            Map<String, Object> snapshot = sourceBucket.toJsonCompatibleSnapshot();

            LocalBucket deserializedBucket = LocalBucket.fromJsonCompatibleSnapshot(snapshot);
            assertTrue(ComparableByContent.equals(sourceBucket, deserializedBucket));
        }
    }

}
