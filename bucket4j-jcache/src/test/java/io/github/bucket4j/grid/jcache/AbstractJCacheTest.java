/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
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

package io.github.bucket4j.grid.jcache;


import io.github.bucket4j.AbstractDistributedBucketTest;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.Backend;
import org.junit.Test;

import javax.cache.Cache;
import java.time.Duration;
import java.util.UUID;

public abstract class AbstractJCacheTest extends AbstractDistributedBucketTest {

    @Test(expected = UnsupportedOperationException.class)
    public void testThatAsyncNotSupported() {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build();

        getBackend().builder()
                .buildAsyncProxy(UUID.randomUUID().toString(), configuration);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testThatAsyncNotSupported_2() {
        getBackend().getProxyConfigurationAsync("42");
    }

    @Override
    protected Backend<String> getBackend() {
        return new JCacheBackend<>(getCache());
    }

    @Override
    protected void removeBucketFromBackingStorage(String key) {
        getCache().remove(key);
    }

    protected abstract Cache<String, byte[]> getCache();

}
