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

package io.github.bucket4j.distributed.proxy.synchronization.per_bucket.batch;

import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronizationListener;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.MultiCommand;
import io.github.bucket4j.distributed.remote.MultiResult;
import io.github.bucket4j.util.concurrent.batch.AsyncBatchHelper;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

public class AsyncBatchingExecutor implements AsyncCommandExecutor {

    private final AsyncBatchHelper<RemoteCommand<?>, CommandResult<?>, MultiCommand, CommandResult<MultiResult>> batchingHelper;
    private final AsyncCommandExecutor wrappedExecutor;


    public AsyncBatchingExecutor(AsyncCommandExecutor originalExecutor, BucketSynchronizationListener listener) {
        this.wrappedExecutor = originalExecutor;
        BiFunction<MultiCommand, CommandResult<MultiResult>, List<CommandResult<?>>> combinedResultSplitter = new BiFunction<>() {
            @Override
            public List<CommandResult<?>> apply(MultiCommand multiCommand, CommandResult<MultiResult> multiResult) {
                return multiCommand.unwrap(multiResult);
            }
        };
        Function<RemoteCommand<?>, CompletableFuture<CommandResult<?>>> taskExecutor = new Function<>() {
            @Override
            public CompletableFuture<CommandResult<?>> apply(RemoteCommand<?> remoteCommand) {
                CompletableFuture<? extends CommandResult<?>> future = wrappedExecutor.executeAsync(remoteCommand);
                return (CompletableFuture<CommandResult<?>>) future;
            }
        };
        Function<MultiCommand, CompletableFuture<CommandResult<MultiResult>>> combinedTaskExecutor = new Function<>() {
            @Override
            public CompletableFuture<CommandResult<MultiResult>> apply(MultiCommand multiCommand) {
                return wrappedExecutor.executeAsync(multiCommand);
            }
        };
        Function<List<RemoteCommand<?>>, MultiCommand> taskCombiner = new Function<>() {
            @Override
            public MultiCommand apply(List<RemoteCommand<?>> commands) {
                if (commands.size() > 1) {
                    listener.incrementMergeCount(commands.size() - 1);
                }
                return MultiCommand.merge(commands);
            }
        };
        this.batchingHelper = AsyncBatchHelper.create(taskCombiner, combinedTaskExecutor, taskExecutor, combinedResultSplitter);
    }


    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(RemoteCommand<T> command) {
        CompletableFuture<T> future = (CompletableFuture<T>) batchingHelper.executeAsync(command);
        return (CompletableFuture<CommandResult<T>>) future;
    }

}
