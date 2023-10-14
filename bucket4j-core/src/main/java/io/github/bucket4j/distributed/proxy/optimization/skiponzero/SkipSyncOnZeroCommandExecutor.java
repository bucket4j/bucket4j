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
package io.github.bucket4j.distributed.proxy.optimization.skiponzero;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.optimization.OptimizationListener;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MultiResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.CreateSnapshotCommand;
import io.github.bucket4j.distributed.remote.commands.MultiCommand;

class SkipSyncOnZeroCommandExecutor implements CommandExecutor, AsyncCommandExecutor {

    private static final int ORIGINAL_COMMAND_INDEX = 0;
    private static final int GET_SNAPSHOT_COMMAND_INDEX = 1;

    private final CommandExecutor originalExecutor;
    private final AsyncCommandExecutor originalAsyncExecutor;
    private final OptimizationListener listener;
    private final TimeMeter timeMeter;

    private RemoteBucketState state;

    private long lastSyncTimeNanos;

    SkipSyncOnZeroCommandExecutor(CommandExecutor originalExecutor, OptimizationListener listener, TimeMeter timeMeter) {
        this.originalExecutor = originalExecutor;
        this.originalAsyncExecutor = null;
        this.listener = listener;
        this.timeMeter = timeMeter;
    }

    SkipSyncOnZeroCommandExecutor(AsyncCommandExecutor originalAsyncExecutor, OptimizationListener listener, TimeMeter timeMeter) {
        this.originalExecutor = null;
        this.originalAsyncExecutor = originalAsyncExecutor;
        this.listener = listener;
        this.timeMeter = timeMeter;
    }

    @Override
    public <T> CommandResult<T> execute(RemoteCommand<T> command) {
        CommandResult<T> localResult = tryExecuteLocally(command);
        if (localResult != null) {
            // remote call is not needed
            listener.incrementSkipCount(1);
            return localResult;
        }

        MultiCommand remoteCommand = prepareRemoteCommand(command);
        CommandResult<MultiResult> remoteResult = originalExecutor.execute(remoteCommand);
        rememberRemoteCommandResult(remoteResult);
        return remoteResult.isError() ?
            (CommandResult<T>) remoteResult :
            (CommandResult<T>) remoteResult.getData().getResults().get(ORIGINAL_COMMAND_INDEX);
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(RemoteCommand<T> command) {
        CommandResult<T> result = tryExecuteLocally(command);
        if (result != null) {
            // remote call is not needed
            listener.incrementSkipCount(1);
            return CompletableFuture.completedFuture(result);
        }

        MultiCommand remoteCommand = prepareRemoteCommand(command);
        CompletableFuture<CommandResult<MultiResult>> resultFuture = originalAsyncExecutor.executeAsync(remoteCommand);
        return resultFuture.thenApply((CommandResult<MultiResult> remoteResult) -> {
            rememberRemoteCommandResult(remoteResult);
            return remoteResult.isError() ?
                (CommandResult<T>) remoteResult :
                (CommandResult<T>) remoteResult.getData().getResults().get(ORIGINAL_COMMAND_INDEX);
        });
    }

    private <T> CommandResult<T> tryExecuteLocally(RemoteCommand<T> command) {
        long currentTimeNanos = timeMeter.currentTimeNanos();

        long commandConsumeTokens = command.estimateTokensToConsume();
        if (isNeedToExecuteRemoteImmediately(command, commandConsumeTokens, currentTimeNanos)) {
            return null;
        }

        // execute local command
        MutableBucketEntry entry = new MutableBucketEntry(state.copy());
        CommandResult<T> result = command.execute(entry, currentTimeNanos);
        long locallyConsumedTokens = command.getConsumedTokens(result.getData());
        if (locallyConsumedTokens == Long.MAX_VALUE) {
            return null;
        }
        if (!isLocalExecutionResultSatisfiesThreshold(locallyConsumedTokens)) {
            return null;
        }

        if (entry.isStateModified()) {
            state = entry.get();
        }
        if (locallyConsumedTokens > 0) {
            // something can be consumed, it needs to execute request on server
            return null;
        }
        return result;
    }

    private boolean isLocalExecutionResultSatisfiesThreshold(long locallyConsumedTokens) {
        if (locallyConsumedTokens == Long.MAX_VALUE || locallyConsumedTokens < 0) {
            // math overflow
            return false;
        }
        return true;
    }

    private <T> boolean isNeedToExecuteRemoteImmediately(RemoteCommand<T> command, long commandConsumeTokens, long currentTimeNanos) {
        if (state == null) {
            // was never synchronized before
            return true;
        }

        if (commandConsumeTokens == 0) {
            // it is not consumption command
            return true;
        }

        if (command.isImmediateSyncRequired(0, currentTimeNanos - lastSyncTimeNanos)) {
            // need to execute immediately because of special command
            return true;
        }

        if (commandConsumeTokens == Long.MAX_VALUE || commandConsumeTokens < 0) {
            // math overflow
            return true;
        }

        return false;
    }

    private <T> MultiCommand prepareRemoteCommand(RemoteCommand<T> command) {
        List<RemoteCommand<?>> commands = new ArrayList<>(3);
        commands.add(command);
        commands.add(new CreateSnapshotCommand());
        return new MultiCommand(commands);
    }

    private void rememberRemoteCommandResult(CommandResult<MultiResult> remoteResult) {
        lastSyncTimeNanos = timeMeter.currentTimeNanos();
        CommandResult<?> snapshotResult = remoteResult.isError() ? remoteResult : remoteResult.getData().getResults().get(GET_SNAPSHOT_COMMAND_INDEX);
        if (snapshotResult.isError()) {
            state = null;
            return;
        }
        this.state = (RemoteBucketState) snapshotResult.getData();
    }


}
