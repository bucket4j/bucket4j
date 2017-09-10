/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
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

package io.github.bucket4j.grid.jcache;

import io.github.bucket4j.Extension;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;

import javax.cache.Cache;
import java.io.Serializable;

/**
 * The extension of Bucket4j library addressed to support <a href="https://www.jcp.org/en/jsr/detail?id=107">JCache API (JSR 107)</a> specification.
 */
public class JCache implements Extension<JCacheBucketBuilder> {

    /**
     * {@inheritDoc}
     *
     * @return new instance of {@link JCacheBucketBuilder}
     */
    @Override
    public JCacheBucketBuilder builder() {
        return new JCacheBucketBuilder();
    }

    /**
     * Creates {@link JCacheProxyManager} for specified cache.
     *
     * @param cache cache for storing state of buckets
     * @param <T> type of keys in the cache
     * @return {@link ProxyManager} for specified cache.
     */
    public <T extends Serializable> ProxyManager<T> proxyManagerForCache(Cache<T, GridBucketState> cache) {
        return new JCacheProxyManager<>(cache);
    }

}
