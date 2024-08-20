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

package io.github.bucket4j.grid.coherence;


import com.tangosol.net.NamedCache;

import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.versioning.Version;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;


/**
 * The extension of Bucket4j library addressed to support <a href="https://www.oracle.com/technetwork/middleware/coherence/overview/index.html">Oracle Coherence</a> in-memory computing platform.
 *
 * @param <K>
 */
public class CoherenceProxyManager<K> extends AbstractProxyManager<K> {

    private final NamedCache<K, byte[]> cache;

    CoherenceProxyManager(Bucket4jCoherence.CoherenceProxyManagerBuilder<K> builder) {
        super(builder.getProxyManagerConfig());
        this.cache = builder.cache;
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        CoherenceProcessor<K, T> entryProcessor = new CoherenceProcessor<>(request);
        byte[] resultBytes = cache.invoke(key, entryProcessor);
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        return deserializeResult(resultBytes, backwardCompatibilityVersion);
    }

    @Override
    public void removeProxy(K key) {
        cache.remove(key);
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

}
