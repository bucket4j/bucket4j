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

/**
 * Monitoring listener for applied optimizations
 */
public interface OptimizationListener {

    /**
     * Creates new instance of {@link DefaultOptimizationListener}
     *
     * @return new instance of {@link DefaultOptimizationListener}
     *
     * @see DefaultOptimizationListener
     */
    static OptimizationListener createDefault() {
        return new DefaultOptimizationListener();
    }

    /**
     * This method is invoked every time when several independent requests to the same bucket combined into a single one.
     *
     * @param count number of requests that were merged
     */
    void incrementMergeCount(int count);

    /**
     * This method is invoked every time when several requests were not propagated to external storage because optimization had decided that they can be served locally.
     *
     * @param count number of requests that were served locally without synchronization with external storage
     */
    void incrementSkipCount(int count);

}
