/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
/*
 *
 *   Copyright 2015-2019 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.bucket4j.grid.coherence;

import com.tangosol.net.NamedCache;
import io.github.bucket4j.Extension;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

/**
 * The extension of Bucket4j library addressed to support <a href="https://www.oracle.com/technetwork/middleware/coherence/overview/index.html">Oracle Coherence</a> in-memory computing platform.
 *
 * Use this extension only if you need in asynchronous API, else stay at {@link io.github.bucket4j.grid.jcache.JCache}
 */
public class Coherence implements Extension<CoherenceBucketBuilder> {

    /**
     * {@inheritDoc}
     *
     * @return new instance of {@link CoherenceBucketBuilder}
     */
    @Override
    public CoherenceBucketBuilder builder() {
        return new CoherenceBucketBuilder();
    }

    /**
     * Creates {@link CoherenceProxyManager} for specified cache.
     *
     * @param cache cache for storing state of buckets
     * @param <T> type of keys in the cache
     * @return {@link ProxyManager} for specified cache.
     */
    public <T extends Serializable> ProxyManager<T> proxyManagerForCache(NamedCache<T, GridBucketState> cache) {
        return new CoherenceProxyManager<>(cache);
    }

    @Override
    public Collection<SerializationHandle<?>> getSerializers() {
        return Collections.singleton(CoherenceEntryProcessorAdapter.SERIALIZATION_HANDLE);
    }

}
