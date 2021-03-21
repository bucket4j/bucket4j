/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2021 Vladimir Bukhtoyarov
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
package io.github.bucket4j.grid.ignite.thin;

import io.github.bucket4j.grid.jcache.JCacheEntryProcessor;

import java.io.Serializable;

public class Bucket4jComputeTaskParams<K extends Serializable, T extends Serializable> implements Serializable {

    private final String cacheName;
    private final K key;
    private final JCacheEntryProcessor<K, T> processor;

    public Bucket4jComputeTaskParams(String cacheName, K key, JCacheEntryProcessor<K, T> processor) {
        this.cacheName = cacheName;
        this.key = key;
        this.processor = processor;
    }

    public JCacheEntryProcessor<K, T> getProcessor() {
        return processor;
    }

    public K getKey() {
        return key;
    }

    public String getCacheName() {
        return cacheName;
    }

    @Override
    public String toString() {
        return "Bucket4jComputeTaskParams{" +
                "cacheName='" + cacheName + '\'' +
                ", key=" + key +
                ", processor=" + processor +
                '}';
    }

}
