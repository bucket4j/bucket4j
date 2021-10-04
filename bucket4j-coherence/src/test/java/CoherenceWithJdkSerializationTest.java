import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import io.github.bucket4j.AbstractDistributedBucketTest;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.grid.RecoveryStrategy;
import io.github.bucket4j.grid.coherence.Coherence;
import io.github.bucket4j.grid.coherence.CoherenceBucketBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.littlegrid.ClusterMemberGroup;
import org.littlegrid.ClusterMemberGroupUtils;

import static junit.framework.TestCase.assertEquals;

public class CoherenceWithJdkSerializationTest extends AbstractDistributedBucketTest<CoherenceBucketBuilder, Coherence> {

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
    protected Class<Coherence> getExtensionClass() {
        return Coherence.class;
    }

    @Override
    protected Bucket build(CoherenceBucketBuilder builder, String key, RecoveryStrategy recoveryStrategy) {
        return builder.build(cache, key, recoveryStrategy);
    }

    @Override
    protected ProxyManager<String> newProxyManager() {
        return Bucket4j.extension(getExtensionClass()).proxyManagerForCache(cache);
    }

    @Override
    protected void removeBucketFromBackingStorage(String key) {
        cache.remove(key);
    }

    @Test(expected = IllegalArgumentException.class)
    @Override
    public void testThatImpossibleToPassNullCacheToProxyManagerConstructor() {
        Bucket4j.extension(getExtensionClass()).proxyManagerForCache(null);
    }

}
