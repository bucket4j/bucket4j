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
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.remote.Backend;
import io.github.bucket4j.remote.ProxyManager;
import io.github.bucket4j.remote.RecoveryStrategy;
import io.github.bucket4j.remote.RemoteBucketState;
import org.junit.Test;

import javax.cache.Cache;
import java.time.Duration;
import java.util.UUID;

import static io.github.bucket4j.remote.RecoveryStrategy.RECONSTRUCT;

public abstract class AbstractJCacheTest extends AbstractDistributedBucketTest {

    @Test(expected = UnsupportedOperationException.class)
    public void testThatAsyncNotSupported() {
        Bucket bucket = Bucket4j.builder(getBackend())
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build(UUID.randomUUID().toString(), RECONSTRUCT);

        bucket.asAsync();
    }

    @Override
    protected Backend<String> getBackend() {
        return new JCacheBackend<>(getCache());
    }

    @Override
    protected void removeBucketFromBackingStorage(String key) {
        getCache().remove(key);
    }

    protected abstract Cache<String, RemoteBucketState> getCache();

}
