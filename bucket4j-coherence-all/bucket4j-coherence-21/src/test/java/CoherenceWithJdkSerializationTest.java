import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import io.github.bucket4j.grid.coherence.Bucket4jCoherence;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.littlegrid.ClusterMemberGroup;
import org.littlegrid.ClusterMemberGroupUtils;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CoherenceWithJdkSerializationTest extends AbstractDistributedBucketTest {

    private static ClusterMemberGroup memberGroup;
    private static NamedCache cache;

    @BeforeAll
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

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "CoherenceProxyManager_JdkSerialization",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jCoherence.entryProcessorBasedBuilder(cache)
            ).checkExpiration()
        );
    }

    @AfterAll
    public static void shutdownCache() {
        ClusterMemberGroupUtils.shutdownCacheFactoryThenClusterMemberGroups(memberGroup);
    }

    @Test
    public void testThatCoherenceCanBeStarted() {
        cache.put("13", "42");
        assertEquals("42", cache.get("13"));
    }

}
