package io.github.bucket4j.distributed.proxy.optimization.delay;

import io.github.bucket4j.RemoteVerboseResult;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.optimization.*;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MultiResult;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.ConsumeIgnoringRateLimitsCommand;
import io.github.bucket4j.distributed.remote.commands.MultiCommand;
import io.github.bucket4j.distributed.remote.commands.VerboseCommand;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class DelayedCommandExecutor implements CommandExecutor, AsyncCommandExecutor {

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
        CommandResult<T> result = tryConsumeLocally(command);
        if (result != null) {
            // remote call is not needed
            return result;
        }

        VerboseCommand<MultiResult> remoteCommand = prepareRemoteCommand(command);
        CommandResult<RemoteVerboseResult<MultiResult>> remoteResult = originalExecutor.execute(remoteCommand);

        if (remoteResult.isBucketNotFound()) {
            return CommandResult.bucketNotFound();
        }

        RemoteVerboseResult<MultiResult> verboseMultiResult = remoteResult.getData();
        rememberRemoteCommandResult(remoteCommand, verboseMultiResult);
        return (CommandResult<T>) verboseMultiResult.getValue().getResults().get(1);
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(RemoteCommand<T> command) {
        CommandResult<T> result = tryConsumeLocally(command);
        if (result != null) {
            // remote call is not needed
            listener.incrementSkipCount(1);
            return CompletableFuture.completedFuture(result);
        }

        VerboseCommand<MultiResult> remoteCommand = prepareRemoteCommand(command);
        CompletableFuture<CommandResult<RemoteVerboseResult<MultiResult>>> resultFuture = originalAsyncExecutor.executeAsync(remoteCommand);
        return resultFuture.thenApply(remoteResult -> {
            if (remoteResult.isBucketNotFound()) {
                return CommandResult.bucketNotFound();
            }
            RemoteVerboseResult<MultiResult> verboseMultiResult = remoteResult.getData();
            rememberRemoteCommandResult(remoteCommand, verboseMultiResult);
            return (CommandResult<T>) verboseMultiResult.getValue().getResults().get(1);
        });
    }

    private <T> CommandResult<T> tryConsumeLocally(RemoteCommand<T> command) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        if (isNeedToExecuteRemoteImmediately(command, currentTimeNanos)) {
            return null;
        }

        // execute local command
        InMemoryMutableEntry entry = new InMemoryMutableEntry(state.copy());
        CommandResult<T> result = command.execute(entry, currentTimeNanos);
        long locallyConsumedTokens = CommandInsiders.getConsumedTokens(command, result.getData());
        if (locallyConsumedTokens == Long.MAX_VALUE) {
            return null;
        }

        if (!isLocalExecutionResultSatisfiesThreshold(locallyConsumedTokens)) {
            return null;
        }

        postponedToConsumeTokens += locallyConsumedTokens;
        state = entry.getNewState();

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

        if (CommandInsiders.isImmediateSyncRequired(command)) {
            // need to execute immediately because of special command
            return true;
        }

        long commandTokens = CommandInsiders.estimateTokensToConsume(command);
        if (commandTokens == Long.MAX_VALUE || commandTokens + postponedToConsumeTokens < 0) {
            // math overflow
            return true;
        }

        return commandTokens + postponedToConsumeTokens > delayParameters.maxUnsynchronizedTokens;
    }

    private <T> VerboseCommand<MultiResult> prepareRemoteCommand(RemoteCommand<T> command) {
        List<RemoteCommand<?>> commands = Arrays.asList(
            new ConsumeIgnoringRateLimitsCommand(this.postponedToConsumeTokens),
            command
        );
        MultiCommand multiCommand = new MultiCommand(commands);
        return new VerboseCommand<>(multiCommand);
    }

    private void rememberRemoteCommandResult(VerboseCommand<MultiResult> remoteCommand, RemoteVerboseResult<MultiResult> remoteResult) {
        state = remoteResult.getState();
        postponedToConsumeTokens = 0;
        lastSyncTimeNanos = timeMeter.currentTimeNanos();
    }


}
