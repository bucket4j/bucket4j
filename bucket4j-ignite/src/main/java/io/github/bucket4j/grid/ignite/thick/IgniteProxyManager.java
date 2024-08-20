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

package io.github.bucket4j.grid.ignite.thick;

import org.apache.ignite.IgniteCache;

import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;


/**
 * The extension of Bucket4j library addressed to support <a href="https://ignite.apache.org/">Apache ignite</a> in-memory computing platform.
 */
public class IgniteProxyManager<K> extends AbstractProxyManager<K> {

    private final IgniteCache<K, byte[]> cache;

    IgniteProxyManager(Bucket4jIgniteThick.IgniteProxyManagerBuilder<K> builder) {
        super(builder.getProxyManagerConfig());
        cache = builder.cache;
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        IgniteEntryProcessor<K> entryProcessor = new IgniteEntryProcessor<>(request);
        byte[] resultBytes = cache.invoke(key, entryProcessor);
        return deserializeResult(resultBytes, request.getBackwardCompatibilityVersion());
    }

    @Override
    public void removeProxy(K key) {
        cache.remove(key);
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return false;
    }

}
