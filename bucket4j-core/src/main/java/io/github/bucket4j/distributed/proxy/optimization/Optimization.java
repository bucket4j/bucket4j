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

import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.CommandExecutor;

/**
 * Specifies request optimization strategy for {@link BucketProxy} and {@link AsyncBucketProxy}.
 * By default an interaction with {@link BucketProxy} and {@link AsyncBucketProxy} leads to immediately request to remote storage,
 * various implementations of {@link Optimization} interface can optimize this behavior
 */
public interface Optimization {

    Optimization NONE_OPTIMIZED = new Optimization() {
        @Override
        public Optimization withListener(OptimizationListener listener) {
            return this;
        }

        @Override
        public CommandExecutor apply(CommandExecutor originalExecutor) {
            return originalExecutor;
        }

        @Override
        public AsyncCommandExecutor apply(AsyncCommandExecutor originalExecutor) {
            return originalExecutor;
        }
    };

    /**
     * TODO
     *
     * @param listener
     * @return
     */
    Optimization withListener(OptimizationListener listener);

    /**
     * TODO
     *
     * @param originalExecutor
     * @return
     */
    CommandExecutor apply(CommandExecutor originalExecutor);

    /**
     * Optimizations
     * @param originalExecutor
     * @return
     */
    AsyncCommandExecutor apply(AsyncCommandExecutor originalExecutor);

}
