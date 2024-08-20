package io.github.bucket4j.local;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.util.ComparableByContent;

import static io.github.bucket4j.local.ConcurrencyStrategy.LOCK_FREE;
import static io.github.bucket4j.local.ConcurrencyStrategy.REENTRANT_LOCK_PROTECTED;
import static io.github.bucket4j.local.ConcurrencyStrategy.SYNCHRONIZED;
import static io.github.bucket4j.local.ConcurrencyStrategy.UNSAFE;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class LocalBucketsSerializationTest {

    private static final List<ConcurrencyStrategy> knownTypes = Arrays.asList(LOCK_FREE, SYNCHRONIZED, REENTRANT_LOCK_PROTECTED, UNSAFE);

    @Test
    public void testBinarySerialization() throws IOException {
        for (ConcurrencyStrategy strategy : knownTypes) {
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
        for (ConcurrencyStrategy strategy : knownTypes) {
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
