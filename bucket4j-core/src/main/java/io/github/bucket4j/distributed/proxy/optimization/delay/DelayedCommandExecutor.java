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
package io.github.bucket4j.distributed.proxy.optimization.delay;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.optimization.*;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MultiResult;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.BucketEntryWrapper;
import io.github.bucket4j.distributed.remote.commands.ConsumeIgnoringRateLimitsCommand;
import io.github.bucket4j.distributed.remote.commands.CreateSnapshotCommand;
import io.github.bucket4j.distributed.remote.commands.MultiCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class DelayedCommandExecutor implements CommandExecutor, AsyncCommandExecutor {

    private static final int ORIGINAL_COMMAND_INDEX = 1;
    private static final int GET_SNAPSHOT_COMMAND_INDEX = 2;

    private final CommandExecutor originalExecutor;
    private final AsyncCommandExecutor originalAsyncExecutor;
    private final DelayParameters delayParameters;
    private final OptimizationListener listener;
    private final TimeMeter timeMeter;

    private RemoteBucketState state;

    private long lastSyncTimeNanos;
    private long postponedToConsumeTokens;

    DelayedCommandExecutor(CommandExecutor originalExecutor, DelayParameters delayParameters, OptimizationListener listener, TimeMeter timeMeter) {
        this.originalExecutor = originalExecutor;
        this.originalAsyncExecutor = null;
        this.delayParameters = delayParameters;
        this.listener = listener;
        this.timeMeter = timeMeter;
    }

    DelayedCommandExecutor(AsyncCommandExecutor originalAsyncExecutor, DelayParameters delayParameters, OptimizationListener listener, TimeMeter timeMeter) {
        this.originalExecutor = null;
        this.originalAsyncExecutor = originalAsyncExecutor;
        this.delayParameters = delayParameters;
        this.listener = listener;
        this.timeMeter = timeMeter;
    }

    @Override
    public <T> CommandResult<T> execute(RemoteCommand<T> command) {
        CommandResult<T> localResult = tryConsumeLocally(command);
        if (localResult != null) {
            // remote call is not needed
            listener.incrementSkipCount(1);
            return localResult;
        }

        MultiCommand remoteCommand = prepareRemoteCommand(command);
        MultiResult multiResult = originalExecutor.execute(remoteCommand).getData();
        rememberRemoteCommandResult(multiResult);
        return (CommandResult<T>) multiResult.getResults().get(ORIGINAL_COMMAND_INDEX);
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(RemoteCommand<T> command) {
        CommandResult<T> result = tryConsumeLocally(command);
        if (result != null) {
            // remote call is not needed
            listener.incrementSkipCount(1);
            return CompletableFuture.completedFuture(result);
        }

        MultiCommand remoteCommand = prepareRemoteCommand(command);
        CompletableFuture<CommandResult<MultiResult>> resultFuture = originalAsyncExecutor.executeAsync(remoteCommand);
        return resultFuture.thenApply(remoteResult -> {
            MultiResult multiResult = remoteResult.getData();
            rememberRemoteCommandResult(multiResult);
            return (CommandResult<T>) multiResult.getResults().get(ORIGINAL_COMMAND_INDEX);
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
        return postponedToConsumeTokens + locallyConsumedTokens <= delayParameters.maxUnsynchronizedTokens;
    }

    private <T> boolean isNeedToExecuteRemoteImmediately(RemoteCommand<T> command, long currentTimeNanos) {
        if (state == null) {
            // was never synchronized before
            return true;
        }

        if (currentTimeNanos - lastSyncTimeNanos  > delayParameters.maxUnsynchronizedTimeoutNanos) {
            // too long period passed since last sync
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

        return commandTokens + postponedToConsumeTokens > delayParameters.maxUnsynchronizedTokens;
    }

    private <T> MultiCommand prepareRemoteCommand(RemoteCommand<T> command) {
        List<RemoteCommand<?>> commands = new ArrayList<>(3);
        commands.add(new ConsumeIgnoringRateLimitsCommand(this.postponedToConsumeTokens));
        commands.add(command);
        commands.add(new CreateSnapshotCommand());
        return new MultiCommand(commands);
    }

    private void rememberRemoteCommandResult(MultiResult multiResult) {
        postponedToConsumeTokens = 0;
        lastSyncTimeNanos = timeMeter.currentTimeNanos();
        CommandResult<?> snapshotResult = multiResult.getResults().get(GET_SNAPSHOT_COMMAND_INDEX);
        if (snapshotResult.isBucketNotFound()) {
            state = null;
            return;
        }
        this.state = (RemoteBucketState) snapshotResult.getData();
    }


}
