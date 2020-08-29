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
package io.github.bucket4j.distributed.proxy.optimization;

import java.util.concurrent.atomic.AtomicLong;

public class DefaultOptimizationListener implements OptimizationListener {

    private final AtomicLong mergeCount = new AtomicLong();
    private final AtomicLong skipCount = new AtomicLong();

    @Override
    public void incrementMergeCount(int count) {
        mergeCount.addAndGet(count);
    }

    @Override
    public void incrementSkipCount(int count) {
        skipCount.addAndGet(count);
    }

    public long getMergeCount() {
        return mergeCount.get();
    }

    public long getSkipCount() {
        return skipCount.get();
    }

}
