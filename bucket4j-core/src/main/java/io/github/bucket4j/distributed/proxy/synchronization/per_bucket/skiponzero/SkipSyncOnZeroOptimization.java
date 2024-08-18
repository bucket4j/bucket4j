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

package io.github.bucket4j.distributed.proxy.synchronization.per_bucket.skiponzero;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.DelayParameters;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.Optimization;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.OptimizationListener;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.batch.AsyncBatchingExecutor;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.batch.BatchingExecutor;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.batch.BatchingOptimization;

/**
 * Optimization that can serve requests locally without synchronization with external storage when it detects that there is no tokens in remote storage,
 * in such case synchronization with storage is being postponed to proposed refill of first token.
 * This optimization is based on top of {@link BatchingOptimization}, so multiple parallel request to same bucket are grouped.
 *
 * <p>Usage of this optimization can lead to temporal under-consumption in case of tokens are being added to bucket via API like {@link Bucket#addTokens(long)} or {@link Bucket#reset()}.
 *
 * @see DelayParameters
 */
public class SkipSyncOnZeroOptimization implements Optimization {

    private final OptimizationListener listener;
    private final TimeMeter timeMeter;

    public SkipSyncOnZeroOptimization(OptimizationListener listener, TimeMeter timeMeter) {
        this.timeMeter = timeMeter;
        this.listener = listener;
    }

    @Override
    public Optimization withListener(OptimizationListener listener) {
        return new SkipSyncOnZeroOptimization(listener, timeMeter);
    }

    @Override
    public CommandExecutor apply(CommandExecutor originalExecutor) {
        SkipSyncOnZeroCommandExecutor predictiveCommandExecutor = new SkipSyncOnZeroCommandExecutor(originalExecutor, listener, timeMeter);
        return new BatchingExecutor(predictiveCommandExecutor, listener);
    }

    @Override
    public AsyncCommandExecutor apply(AsyncCommandExecutor originalExecutor) {
        SkipSyncOnZeroCommandExecutor predictiveCommandExecutor = new SkipSyncOnZeroCommandExecutor(originalExecutor, listener, timeMeter);
        return new AsyncBatchingExecutor(predictiveCommandExecutor, listener);
    }

}
