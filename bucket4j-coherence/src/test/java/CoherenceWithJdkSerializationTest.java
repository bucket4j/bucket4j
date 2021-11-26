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
        if (System.getenv("CI") == null) {
            memberGroup = ClusterMemberGroupUtils.newBuilder().setStorageEnabledCount(2)
                    .setCacheConfiguration("test-coherence-config.xml")
                    .buildAndConfigureForStorageDisabledClient();
        } else {
            // Use less nodes on Github Actions build environment in order to satisfy the 7Gb limit
            // https://docs.github.com/en/actions/using-github-hosted-runners/about-github-hosted-runners#supported-runners-and-hardware-resources
            // https://docs.github.com/en/actions/learn-github-actions/environment-variables#default-environment-variables
            memberGroup = ClusterMemberGroupUtils.newBuilder()
                    .setStorageEnabledCount(0)
                    .buildAndConfigureFor(ClusterMemberGroup.BuildAndConfigureEnum.STORAGE_ENABLED_MEMBER);
        }

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
