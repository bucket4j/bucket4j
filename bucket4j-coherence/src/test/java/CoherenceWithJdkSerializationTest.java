import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.grid.coherence.CoherenceProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.littlegrid.ClusterMemberGroup;
import org.littlegrid.ClusterMemberGroupUtils;

import java.util.UUID;

import static junit.framework.TestCase.assertEquals;

public class CoherenceWithJdkSerializationTest extends AbstractDistributedBucketTest<String> {

    private static ClusterMemberGroup memberGroup;
    private static NamedCache cache;

    @BeforeClass
    public static void prepareCache() throws InterruptedException {
        memberGroup = ClusterMemberGroupUtils.newBuilder().setStorageEnabledCount(2)
                .setCacheConfiguration("test-coherence-jdk_serialization-config.xml")
                .buildAndConfigureForStorageDisabledClient();
        cache = CacheFactory.getCache("my_buckets");
    }

    @AfterClass
    public static void shutdownCache() {
        ClusterMemberGroupUtils.shutdownCacheFactoryThenClusterMemberGroups(memberGroup);
    }

    @Test
    public void testThatCoherenceCanBeStarted() {
        cache.put("13", "42");
        assertEquals("42", cache.get("13"));
    }

    @Override
    protected ProxyManager<String> getProxyManager() {
        return new CoherenceProxyManager(cache, ClientSideConfig.getDefault());
    }

    @Override
    protected String generateRandomKey() {
        return UUID.randomUUID().toString();
    }

}
