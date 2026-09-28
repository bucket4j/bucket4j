import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import io.github.bucket4j.grid.coherence.Bucket4jCoherence;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

public class CoherenceWithPofSerializationTest extends AbstractDistributedBucketTest {

    private static final int WKA_PORT = 9783;
    private static final int CLIENT_PORT = 9784;

    private static Cloud cloud;
    private static ViNode server;
    private static NamedCache<String, byte[]> cache;

    @BeforeAll
    public static void prepareCache() {
        // start a storage-enabled Coherence cluster member in a separate JVM on the current host
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
        server = cloud.node("stateful-coherence-server");
        server.exec((Runnable & Serializable) () -> {
            configureCoherence(WKA_PORT, WKA_PORT, true);
            CacheFactory.getCache("my_buckets");
        });

        // storage-disabled Coherence cluster member which works inside current JVM and does not hold data
        configureCoherence(CLIENT_PORT, WKA_PORT, false);
        cache = CacheFactory.getCache("my_buckets");

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "CoherenceProxyManager_PofSerialization",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jCoherence.entryProcessorBasedBuilder(cache)
            ).checkExpiration()
        );
    }

    private static void configureCoherence(int localPort, int wkaPort, boolean storageEnabled) {
        System.setProperty("coherence.cluster", "bucket4j-coherence-pof-test");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.localport", String.valueOf(localPort));
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.wka.port", String.valueOf(wkaPort));
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.distributed.localstorage", String.valueOf(storageEnabled));
        System.setProperty("coherence.cacheconfig", "test-coherence-config.xml");
    }

    @AfterAll
    public static void shutdownCache() {
        CacheFactory.shutdown();
        if (server != null) {
            server.exec((Runnable & Serializable) CacheFactory::shutdown);
        }
        if (cloud != null) {
            cloud.shutdown();
        }
    }

}
