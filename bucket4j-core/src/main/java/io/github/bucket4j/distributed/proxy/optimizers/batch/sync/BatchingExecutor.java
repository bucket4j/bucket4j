/*
 *
 * Copyright 2015-2020 Vladimir Bukhtoyarov
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

package io.github.bucket4j.distributed.proxy.optimizers.batch.sync;

import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.optimizers.batch.TaskQueue;
import io.github.bucket4j.distributed.proxy.optimizers.batch.WaitingTask;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.MultiCommand;
import io.github.bucket4j.distributed.remote.MultiResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BatchingExecutor implements CommandExecutor {

    private final CommandExecutor wrappedExecutor;

    private final TaskQueue taskQueue = new TaskQueue();

    public BatchingExecutor(CommandExecutor originalExecutor) {
        this.wrappedExecutor = originalExecutor;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(RemoteCommand<T> command) {
        WaitingTask waitingNode = taskQueue.lockExclusivelyOrEnqueue(command);

        if (waitingNode == null) {
            try {
                return wrappedExecutor.execute(command);
            } finally {
                taskQueue.wakeupAnyThreadFromNextBatchOrFreeLock();
            }
        }

        CommandResult<?> result = waitingNode.waitUninterruptedly();
        if (result != WaitingTask.NEED_TO_EXECUTE_NEXT_BATCH) {
            // our future completed by another thread from current batch
            return (CommandResult<T>) result;
        }

        // current thread is responsible to execute the batch of commands
        try {
            return executeBatch(waitingNode);
        } finally {
            taskQueue.wakeupAnyThreadFromNextBatchOrFreeLock();
        }
    }

    private <T extends Serializable> CommandResult<T> executeBatch(WaitingTask currentWaitingNode) {
        List<WaitingTask> waitingNodes = taskQueue.takeAllWaitingTasksOrFreeLock();

        if (waitingNodes.size() == 1) {
            RemoteCommand<?> singleCommand = waitingNodes.get(0).command;
            return (CommandResult<T>) wrappedExecutor.execute(singleCommand);
        }

        try {
            int resultIndex = -1;
            List<RemoteCommand<?>> commandsInBatch = new ArrayList<>(waitingNodes.size());
            for (int i = 0; i < waitingNodes.size(); i++) {
                WaitingTask waitingNode = waitingNodes.get(i);
                commandsInBatch.add(waitingNode.command);
                if (waitingNode == currentWaitingNode) {
                    resultIndex = i;
                }
            }
            MultiCommand multiCommand = new MultiCommand(commandsInBatch);

            CommandResult<MultiResult> multiResult = wrappedExecutor.execute(multiCommand);
            List<CommandResult<?>> singleResults = multiResult.getData().getResults();
            for (int i = 0; i < waitingNodes.size(); i++) {
                CommandResult<?> singleResult = singleResults.get(i);
                waitingNodes.get(i).future.complete(singleResult);
            }

            return (CommandResult<T>) singleResults.get(resultIndex);
        } catch (Throwable e) {
            for (WaitingTask waitingNode : waitingNodes) {
                waitingNode.future.completeExceptionally(e);
            }
            throw new BatchFailedException(e);
        }
    }

}
