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
import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.optimization.OptimizationListener;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.remote.commands.ConsumeIgnoringRateLimitsCommand;
import io.github.bucket4j.distributed.remote.commands.CreateSnapshotCommand;
import io.github.bucket4j.distributed.remote.commands.MultiCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

class ManuallySyncingCommandExecutor implements CommandExecutor, AsyncCommandExecutor {

    private static final int ORIGINAL_COMMAND_INDEX = 1;
    private static final int GET_SNAPSHOT_COMMAND_INDEX = 2;

    private final CommandExecutor originalExecutor;
    private final AsyncCommandExecutor originalAsyncExecutor;
    private final OptimizationListener listener;
    private final TimeMeter timeMeter;

    private RemoteBucketState state;

    private long lastSyncTimeNanos;
    private long postponedToConsumeTokens;

    private final ReentrantLock localStateMutationLock = new ReentrantLock();
    private final ReentrantLock remoteExecutionLock = new ReentrantLock();
    private CompletableFuture<?> inProgressSynchronizationFuture;

    ManuallySyncingCommandExecutor(CommandExecutor originalExecutor, OptimizationListener listener, TimeMeter timeMeter) {
        this.originalExecutor = originalExecutor;
        this.originalAsyncExecutor = null;
        this.listener = listener;
        this.timeMeter = timeMeter;
    }

    ManuallySyncingCommandExecutor(AsyncCommandExecutor originalAsyncExecutor, OptimizationListener listener, TimeMeter timeMeter) {
        this.originalExecutor = null;
        this.originalAsyncExecutor = originalAsyncExecutor;
        this.listener = listener;
        this.timeMeter = timeMeter;
    }

    @Override
    public <T> CommandResult<T> execute(RemoteCommand<T> command) {
        MultiCommand remoteCommand;
        localStateMutationLock.lock();
        try {
            CommandResult<T> localResult = tryConsumeLocally(command);
            if (localResult != null) {
                // remote call is not needed
                listener.incrementSkipCount(1);
                return localResult;
            } else {
                remoteCommand = prepareRemoteCommand(command);
            }
        } finally {
            localStateMutationLock.unlock();
        }

        remoteExecutionLock.lock();
        try {
            CommandResult<MultiResult> remoteResult = originalExecutor.execute(remoteCommand);
            rememberRemoteCommandResult(remoteResult);
            return remoteResult.isError() ?
                (CommandResult<T>) remoteResult :
                (CommandResult<T>) remoteResult.getData().getResults().get(ORIGINAL_COMMAND_INDEX);
        } finally {
            remoteExecutionLock.unlock();
        }
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(RemoteCommand<T> command) {
        MultiCommand remoteCommand;
        localStateMutationLock.lock();
        try {
            CommandResult<T> result = tryConsumeLocally(command);
            if (result != null) {
                // remote call is not needed
                listener.incrementSkipCount(1);
                return CompletableFuture.completedFuture(result);
            } else {
                remoteCommand = prepareRemoteCommand(command);
            }
        } finally {
            localStateMutationLock.unlock();
        }

        remoteExecutionLock.lock();
        CompletableFuture<CommandResult<MultiResult>> resultFuture;
        try {
            if (inProgressSynchronizationFuture == null) {
                resultFuture = originalAsyncExecutor.executeAsync(remoteCommand);
            } else {
                resultFuture = inProgressSynchronizationFuture.thenCompose(f -> {
                    assert originalAsyncExecutor != null;
                    return originalAsyncExecutor.executeAsync(remoteCommand);
                });
            }
            inProgressSynchronizationFuture = resultFuture;
        } finally {
            remoteExecutionLock.unlock();
        }

        return resultFuture.thenApply((CommandResult<MultiResult> remoteResult) -> {
            rememberRemoteCommandResult(remoteResult);
            return remoteResult.isError() ?
                (CommandResult<T>) remoteResult :
                (CommandResult<T>) remoteResult.getData().getResults().get(ORIGINAL_COMMAND_INDEX);
        });
    }

    private <T> CommandResult<T> tryConsumeLocally(RemoteCommand<T> command) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        if (isNeedToExecuteRemoteImmediately(command, currentTimeNanos)) {
            return null;
        }

        // execute local command
        BucketEntryWrapper entry = new BucketEntryWrapper(state.copy());
        CommandResult<T> result = command.execute(entry, currentTimeNanos);
        long locallyConsumedTokens = command.getConsumedTokens(result.getData());
        if (locallyConsumedTokens == Long.MAX_VALUE) {
            return null;
        }

        if (!isLocalExecutionResultSatisfiesThreshold(locallyConsumedTokens)) {
            return null;
        }

        postponedToConsumeTokens += locallyConsumedTokens;
        if (entry.isStateModified()) {
            state = entry.get();
        }

        return result;
    }

    private boolean isLocalExecutionResultSatisfiesThreshold(long locallyConsumedTokens) {
        if (locallyConsumedTokens == Long.MAX_VALUE || postponedToConsumeTokens + locallyConsumedTokens < 0) {
            // math overflow
            return false;
        }
        return true;
    }

    private <T> boolean isNeedToExecuteRemoteImmediately(RemoteCommand<T> command, long currentTimeNanos) {
        if (state == null) {
            // was never synchronized before
            return true;
        }

        if (command.isImmediateSyncRequired(postponedToConsumeTokens, currentTimeNanos - lastSyncTimeNanos)) {
            // need to execute immediately because of special command
            return true;
        }

        long commandTokens = command.estimateTokensToConsume();
        if (commandTokens == Long.MAX_VALUE || commandTokens + postponedToConsumeTokens < 0) {
            // math overflow
            return true;
        }

        return false;
    }

    private <T> MultiCommand prepareRemoteCommand(RemoteCommand<T> command) {
        List<RemoteCommand<?>> commands = new ArrayList<>(3);
        commands.add(new ConsumeIgnoringRateLimitsCommand(this.postponedToConsumeTokens));
        this.postponedToConsumeTokens = 0;
        commands.add(command);
        commands.add(new CreateSnapshotCommand());
        return new MultiCommand(commands);
    }

    private void rememberRemoteCommandResult(CommandResult<MultiResult> remoteResult) {
        localStateMutationLock.lock();
        try {
            lastSyncTimeNanos = timeMeter.currentTimeNanos();
            CommandResult<?> snapshotResult = remoteResult.isError() ? remoteResult : remoteResult.getData().getResults().get(GET_SNAPSHOT_COMMAND_INDEX);
            if (snapshotResult.isError()) {
                state = null;
                return;
            }
            this.state = (RemoteBucketState) snapshotResult.getData();

            // decrease available tokens by amount that consumed while remote request was in progress
            if (postponedToConsumeTokens > 0) {
                this.state.consume(postponedToConsumeTokens);
            }
        } finally {
            localStateMutationLock.unlock();
        }
    }


}
