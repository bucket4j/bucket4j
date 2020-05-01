/*
 *
 * Copyright 2015-2019 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */


import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import io.github.bucket4j.AbstractDistributedBucketTest;
import io.github.bucket4j.grid.coherence.CoherenceBackend;
import io.github.bucket4j.distributed.proxy.Backend;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.littlegrid.ClusterMemberGroup;
import org.littlegrid.ClusterMemberGroupUtils;


public class CoherenceTest extends AbstractDistributedBucketTest {

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
