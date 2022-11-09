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

package io.github.bucket4j.distributed.proxy.optimization.manual;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.OptimizationController;
import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.optimization.DelayParameters;
import io.github.bucket4j.distributed.proxy.optimization.NopeOptimizationListener;
import io.github.bucket4j.distributed.proxy.optimization.Optimization;
import io.github.bucket4j.distributed.proxy.optimization.OptimizationListener;

/**
 * Optimization that can serve requests locally without synchronization with external storage until explicit call of {@link OptimizationController#syncImmediately()}.
 *
 * @see DelayParameters
 */
public class ManuallySyncingOptimization implements Optimization {

    private final OptimizationListener listener;
    private final TimeMeter timeMeter;

    public ManuallySyncingOptimization() {
        this(NopeOptimizationListener.INSTANCE, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public ManuallySyncingOptimization(OptimizationListener listener, TimeMeter timeMeter) {
        this.timeMeter = timeMeter;
        this.listener = listener;
    }

    @Override
    public Optimization withListener(OptimizationListener listener) {
        return new ManuallySyncingOptimization(listener, timeMeter);
    }

    @Override
    public CommandExecutor apply(CommandExecutor originalExecutor) {
        return new ManuallySyncingCommandExecutor(originalExecutor, listener, timeMeter);
    }

    @Override
    public AsyncCommandExecutor apply(AsyncCommandExecutor originalExecutor) {
        return new ManuallySyncingCommandExecutor(originalExecutor, listener, timeMeter);
    }

}
