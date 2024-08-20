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

package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.config.SerializationConfig;
import com.hazelcast.map.IMap;

import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.grid.hazelcast.Bucket4jHazelcast.HazelcastProxyManagerBuilder;
import io.github.bucket4j.grid.hazelcast.serialization.SerializationUtilities;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;

/**
 * The extension of Bucket4j library addressed to support <a href="https://hazelcast.com//">Hazelcast</a> in-memory data grid.
 */
public class HazelcastProxyManager<K> extends AbstractProxyManager<K> {

    private final IMap<K, byte[]> map;
    private final String offloadableExecutorName;

    HazelcastProxyManager(HazelcastProxyManagerBuilder<K> builder) {
        super(builder.getProxyManagerConfig());
        this.map = builder.map;
        this.offloadableExecutorName = builder.offloadableExecutorName;
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        HazelcastEntryProcessor<K, T> entryProcessor = offloadableExecutorName == null?
                new HazelcastEntryProcessor<>(request) :
                new HazelcastOffloadableEntryProcessor<>(request, offloadableExecutorName);
        byte[] response = map.executeOnKey(key, entryProcessor);
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        return deserializeResult(response, backwardCompatibilityVersion);
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

    @Override
    public void removeProxy(K key) {
        map.remove(key);
    }

    public static void addCustomSerializers(SerializationConfig serializationConfig, final int typeIdBase) {
        SerializationUtilities.addCustomSerializers(serializationConfig, typeIdBase);
    }



}
