/*
 *
 * Copyright 2015-2019 Vladimir Bukhtoyarov
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

package io.github.bucket4j.distributed.proxy.optimizers.batch;

import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.MultiCommand;
import io.github.bucket4j.distributed.remote.MultiResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AsyncBatchingExecutor implements AsyncCommandExecutor {

    private final AsyncCommandExecutor wrappedExecutor;
    private final State state = new State();

    public AsyncBatchingExecutor(AsyncCommandExecutor originalExecutor) {
        this.wrappedExecutor = originalExecutor;
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(RemoteCommand<T> command) {
        WaitingNode waitingNode = null;
        synchronized (state) {
            if (state.executionInProgress) {
                waitingNode = new WaitingNode(command);
                state.waitingCommands.add(waitingNode);
            } else {
                state.executionInProgress = true;
            }
        }

        if (waitingNode != null) {
            return (CompletableFuture) waitingNode.future;
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
        List<WaitingNode> waitingNodes;
        synchronized (state) {
            if (state.waitingCommands.isEmpty()) {
                state.executionInProgress = false;
                return;
            }
            waitingNodes = state.waitingCommands;
            state.waitingCommands = new ArrayList<>();
        }

        try {
            List<RemoteCommand<?>> commandsInBatch = new ArrayList<>(waitingNodes.size());
            for (WaitingNode waitingNode : waitingNodes) {
                commandsInBatch.add(waitingNode.command);
            }
            MultiCommand multiCommand = new MultiCommand(commandsInBatch);

            wrappedExecutor.executeAsync(multiCommand)
                .whenComplete((multiResult, error) -> completeWaitingFutures(waitingNodes, multiResult, error))
                .whenComplete((multiResult, error) -> scheduleNextBatch());
        } catch (Throwable e) {
            try {
                for (WaitingNode waitingNode : waitingNodes) {
                    waitingNode.future.completeExceptionally(e);
                }
            } finally {
                scheduleNextBatch();
            }
        }
    }

    private void completeWaitingFutures(List<WaitingNode> waitingNodes, CommandResult<MultiResult> multiResult, Throwable error) {
        if (error != null) {
            for (WaitingNode waitingNode : waitingNodes) {
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

    private static class State {

        private List<WaitingNode> waitingCommands = new ArrayList<>();

        private boolean executionInProgress;

    }

    private class WaitingNode {

        private final RemoteCommand<?> command;
        private final CompletableFuture<CommandResult> future = new CompletableFuture<>();

        private WaitingNode(RemoteCommand<?> command) {
            this.command = command;
        }

    }

}
