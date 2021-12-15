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
package io.github.bucket4j.grid.ignite.thin.compute;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.resources.IgniteInstanceResource;

public class Bucket4jComputeJob<K> implements ComputeJob {

    private final Bucket4jComputeTaskParams<K> params;

    @IgniteInstanceResource
    private Ignite ignite;

    public Bucket4jComputeJob(Bucket4jComputeTaskParams<K> params) {
        this.params = params;
    }

    @Override
    public Object execute() throws IgniteException {
        IgniteCache<K, byte[]> cache = ignite.cache(params.getCacheName());
        return cache.invoke(params.getKey(), params.getProcessor());
    }

    @Override
    public void cancel() {
        // do nothing
    }

}
