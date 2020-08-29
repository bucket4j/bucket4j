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

package io.github.bucket4j.distributed.proxy.optimization.batch;

import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.optimization.OptimizationListener;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.MultiCommand;
import io.github.bucket4j.distributed.remote.MultiResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AsyncBatchingExecutor implements AsyncCommandExecutor {

    private final AsyncCommandExecutor wrappedExecutor;
    private final OptimizationListener listener;
    private final TaskQueue taskQueue = new TaskQueue();

    public AsyncBatchingExecutor(AsyncCommandExecutor originalExecutor, OptimizationListener listener) {
        this.wrappedExecutor = originalExecutor;
        this.listener = listener;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(RemoteCommand<T> command) {
        WaitingTask waitingTask = taskQueue.lockExclusivelyOrEnqueue(command);

        if (waitingTask != null) {
            // there is another request is in progress, our request will be scheduled later
            return (CompletableFuture) waitingTask.future;
        }

        try {
            return wrappedExecutor.executeAsync(command)
                .whenComplete((result, error) -> scheduleNextBatch());
        } catch (Throwable error) {
            CompletableFuture<CommandResult<T>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(error);
            return failedFuture;
        }
    }

    private void scheduleNextBatch() {
        List<WaitingTask> waitingNodes = taskQueue.takeAllWaitingTasksOrFreeLock();
        if (waitingNodes.isEmpty()) {
            return;
        }

        try {
            listener.incrementMergeCount(waitingNodes.size() - 1);
            List<RemoteCommand<?>> commandsInBatch = new ArrayList<>(waitingNodes.size());
            for (WaitingTask waitingNode : waitingNodes) {
                commandsInBatch.add(waitingNode.command);
            }
            MultiCommand multiCommand = new MultiCommand(commandsInBatch);

            wrappedExecutor.executeAsync(multiCommand)
                .whenComplete((multiResult, error) -> completeWaitingFutures(waitingNodes, multiResult, error))
                .whenComplete((multiResult, error) -> scheduleNextBatch());
        } catch (Throwable e) {
            try {
                for (WaitingTask waitingNode : waitingNodes) {
                    waitingNode.future.completeExceptionally(e);
                }
            } finally {
                scheduleNextBatch();
            }
        }
    }

    private void completeWaitingFutures(List<WaitingTask> waitingNodes, CommandResult<MultiResult> multiResult, Throwable error) {
        if (error != null) {
            for (WaitingTask waitingNode : waitingNodes) {
                try {
                    waitingNode.future.completeExceptionally(error);
                } catch (Throwable t) {
                    waitingNode.future.completeExceptionally(t);
                }
            }
        } else {
            List<CommandResult<?>> singleResults = multiResult.getData().getResults();
            for (int i = 0; i < waitingNodes.size(); i++) {
                try {
                    waitingNodes.get(i).future.complete(singleResults.get(i));
                } catch (Throwable t) {
                    waitingNodes.get(i).future.completeExceptionally(t);
                }
            }
        }
    }

}
