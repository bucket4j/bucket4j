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
