import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.codec.RawBinaryTranscoder;
import com.couchbase.client.java.kv.GetOptions;
import com.couchbase.client.java.kv.ReplaceOptions;

import io.github.bucket4j.couchbase.Bucket4jCouchbase;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.BackwardCompatibilityStateCheckHelper;
import io.github.bucket4j.tck.ProxyManagerSpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

public class CouchbaseTest extends AbstractDistributedBucketTest {

    private static final String BUCKET_NAME = "bucket4j";

    private static CouchbaseContainer couchbaseContainer;
    private static Cluster cluster;

    @BeforeAll
    public static void setupCouchbase() {
        couchbaseContainer = new CouchbaseContainer("couchbase/server:7.6.2")
            .withBucket(new BucketDefinition(BUCKET_NAME).withPrimaryIndex(false));
        couchbaseContainer.start();

        cluster = Cluster.connect(
            couchbaseContainer.getConnectionString(),
            couchbaseContainer.getUsername(),
            couchbaseContainer.getPassword()
        );

        Bucket bucket = cluster.bucket(BUCKET_NAME);
        bucket.waitUntilReady(Duration.ofMinutes(2));
        Collection collection = bucket.defaultCollection();
        var asyncCollection = collection.async();

        specs = List.of(
            new ProxyManagerSpec<>(
                "CouchbaseCompareAndSwapBasedProxyManager",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jCouchbase.compareAndSwapBasedBuilder(collection)
            ).checkExpiration().checkStateBackwardCompatibility(new BackwardCompatibilityStateCheckHelper<String>() {
                @Override
                public byte[] getRawState(String key) {
                    return collection.get(key, GetOptions.getOptions().transcoder(RawBinaryTranscoder.INSTANCE)).contentAsBytes();
                }

                @Override
                public void setRawState(String key, byte[] state) {
                    collection.replace(key, state, ReplaceOptions.replaceOptions().transcoder(RawBinaryTranscoder.INSTANCE));
                }
            }),
            new ProxyManagerSpec<>(
                "CouchbaseCompareAndSwapBasedProxyManagerAsyncCollection",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jCouchbase.compareAndSwapBasedBuilder(asyncCollection)
            ).checkExpiration().checkStateBackwardCompatibility(new BackwardCompatibilityStateCheckHelper<String>() {
                @Override
                public byte[] getRawState(String key) {
                    try {
                        return asyncCollection.get(key, GetOptions.getOptions().transcoder(RawBinaryTranscoder.INSTANCE)).get().contentAsBytes();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
                @Override
                public void setRawState(String key, byte[] state) {
                    try {
                        asyncCollection.replace(key, state, ReplaceOptions.replaceOptions().transcoder(RawBinaryTranscoder.INSTANCE)).get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }),
            new ProxyManagerSpec<>(
                "CouchbaseCompareAndSwapBasedProxyManagerWithLongKeys",
                () -> ThreadLocalRandom.current().nextLong(),
                () -> Bucket4jCouchbase.compareAndSwapBasedBuilder(collection, Mapper.LONG)
            ).checkExpiration().withoutBackwardCompatibilityStateChecker(),
            new ProxyManagerSpec<>(
                "CouchbaseCompareAndSwapBasedProxyManagerAsyncCollectionWithLongKeys",
                () -> ThreadLocalRandom.current().nextLong(),
                () -> Bucket4jCouchbase.compareAndSwapBasedBuilder(asyncCollection, Mapper.LONG)
            ).checkExpiration().withoutBackwardCompatibilityStateChecker()
        );
    }

    @AfterAll
    public static void cleanupCouchbase() {
        if (cluster != null) {
            cluster.disconnect();
        }
        if (couchbaseContainer != null) {
            couchbaseContainer.stop();
        }
    }
}
