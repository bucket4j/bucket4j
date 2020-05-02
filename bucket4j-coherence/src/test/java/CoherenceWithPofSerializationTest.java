import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import io.github.bucket4j.AbstractDistributedBucketTest;
import io.github.bucket4j.grid.coherence.CoherenceBackend;
import io.github.bucket4j.distributed.proxy.Backend;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.littlegrid.ClusterMemberGroup;
import org.littlegrid.ClusterMemberGroupUtils;


public class CoherenceWithPofSerializationTest extends AbstractDistributedBucketTest {

    private static ClusterMemberGroup memberGroup;
    private static NamedCache<String, byte[]> cache;

    @BeforeClass
    public static void prepareCache() throws InterruptedException {
        memberGroup = ClusterMemberGroupUtils.newBuilder().setStorageEnabledCount(2)
                .setCacheConfiguration("test-coherence-config.xml")
                .buildAndConfigureForStorageDisabledClient();
        cache = CacheFactory.getCache("my_buckets");
    }

    @AfterClass
    public static void shutdownCache() {
        ClusterMemberGroupUtils.shutdownCacheFactoryThenClusterMemberGroups(memberGroup);
    }

    @Override
    protected Backend<String> getBackend() {
        return new CoherenceBackend<>(cache);
    }

    @Override
    protected void removeBucketFromBackingStorage(String key) {
        cache.remove(key);
    }

}
