package io.github.bucket4j.distributed.proxy.optimization.predictive;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.optimization.*;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.remote.commands.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

class PredictiveCommandExecutor implements CommandExecutor, AsyncCommandExecutor {

    private static final int ORIGINAL_COMMAND_INDEX = 1;
    private static final int GET_SNAPSHOT_COMMAND_INDEX = 2;

    private final CommandExecutor originalExecutor;
    private final AsyncCommandExecutor originalAsyncExecutor;
    private final DelayParameters delayParameters;
    private final OptimizationListener listener;
    private final TimeMeter timeMeter;
    private final Sampling sampling;

    private RemoteBucketState state;
    private long postponedToConsumeTokens;
    private long speculativelyConsumedByPredictionTokens;

    PredictiveCommandExecutor(CommandExecutor originalExecutor, DelayParameters delayParameters,
                              PredictionParameters predictionParameters, OptimizationListener listener,
                              TimeMeter timeMeter) {
        this.originalExecutor = originalExecutor;
        this.originalAsyncExecutor = null;
        this.sampling = new Sampling(predictionParameters);
        this.delayParameters = delayParameters;
        this.listener = listener;
        this.timeMeter = timeMeter;
    }

    PredictiveCommandExecutor(AsyncCommandExecutor originalAsyncExecutor, DelayParameters delayParameters,
                              PredictionParameters predictionParameters, OptimizationListener listener,
                              TimeMeter timeMeter) {
        this.originalExecutor = null;
        this.originalAsyncExecutor = originalAsyncExecutor;
        this.sampling = new Sampling(predictionParameters);
        this.delayParameters = delayParameters;
        this.listener = listener;
        this.timeMeter = timeMeter;
    }

    @Override
    public <T> CommandResult<T> execute(RemoteCommand<T> command) {
        CommandResult<T> result = tryConsumeLocally(command);
        if (result != null) {
            // remote call is not needed
            listener.incrementSkipCount(1);
            return result;
        }

        MultiCommand remoteCommand = prepareRemoteCommand(command);
        MultiResult multiResult = originalExecutor.execute(remoteCommand).getData();
        List<CommandResult<?>> results = multiResult.getResults();
        rememberRemoteCommandResult(remoteCommand, multiResult);
        return (CommandResult<T>) results.get(ORIGINAL_COMMAND_INDEX);
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
            List<CommandResult<?>> results = multiResult.getResults();
            rememberRemoteCommandResult(remoteCommand, multiResult);
            return (CommandResult<T>) results.get(ORIGINAL_COMMAND_INDEX);
        });
    }

    private <T> CommandResult<T> tryConsumeLocally(RemoteCommand<T> command) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        if (isNeedToExecuteRemoteImmediately(command, currentTimeNanos)) {
            return null;
        }


        long predictedConsumptionSinceLastSync = sampling.predictedConsumptionByOthersSinceLastSync(currentTimeNanos);
        if (predictedConsumptionSinceLastSync == Long.MAX_VALUE) {
            return null;
        }

        long predictedConsumptionByOthersSinceLastLocalCall = predictedConsumptionSinceLastSync - speculativelyConsumedByPredictionTokens;
        if (predictedConsumptionByOthersSinceLastLocalCall < 0) {
            predictedConsumptionByOthersSinceLastLocalCall = 0;
        }

        // prepare local command
        ConsumeAsMuchAsPossibleCommand consumeByPredictionCommand = new ConsumeAsMuchAsPossibleCommand(predictedConsumptionByOthersSinceLastLocalCall);
        List<RemoteCommand<?>> commands = Arrays.asList(consumeByPredictionCommand, command);
        MultiCommand multiCommand = new MultiCommand(commands);

        // execute local command
        BucketEntryWrapper entry = new BucketEntryWrapper(state.copy());
        MultiResult multiResult = multiCommand.execute(entry, currentTimeNanos).getData();

        if (multiCommand.getConsumedTokens(multiResult) == Long.MAX_VALUE) {
            return null;
        }

        long locallyConsumedTokens = command.getConsumedTokens((T) multiResult.getResults().get(ORIGINAL_COMMAND_INDEX).getData());
        if (!isLocalExecutionResultSatisfiesThreshold(locallyConsumedTokens)) {
            return null;
        }

        postponedToConsumeTokens += locallyConsumedTokens;
        speculativelyConsumedByPredictionTokens += predictedConsumptionByOthersSinceLastLocalCall;
        if (entry.isStateModified()) {
            state = entry.get();
        }

        return (CommandResult<T>) multiResult.getResults().get(ORIGINAL_COMMAND_INDEX);
    }

    private boolean isLocalExecutionResultSatisfiesThreshold(long locallyConsumedTokens) {
        if (locallyConsumedTokens == Long.MAX_VALUE || postponedToConsumeTokens + locallyConsumedTokens < 0) {
            // math overflow
            return false;
        }
        return postponedToConsumeTokens + locallyConsumedTokens <= delayParameters.maxUnsynchronizedTokens;
    }

    private <T> boolean isNeedToExecuteRemoteImmediately(RemoteCommand<T> command, long currentTimeNanos) {
        if (sampling.isNeedToExecuteRemoteImmediately(currentTimeNanos)) {
            return true;
        }

        long lastSyncTimeNanos = sampling.getLastSyncTimeNanos();
        if (currentTimeNanos - lastSyncTimeNanos > delayParameters.maxUnsynchronizedTimeoutNanos) {
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

    private void rememberRemoteCommandResult(MultiCommand multiCommand, MultiResult multiResult) {
        postponedToConsumeTokens = 0;
        speculativelyConsumedByPredictionTokens = 0;
        CommandResult<?> snapshotResult = multiResult.getResults().get(GET_SNAPSHOT_COMMAND_INDEX);
        if (snapshotResult.isBucketNotFound()) {
            state = null;
            sampling.clear();
            return;
        }
        state = (RemoteBucketState) snapshotResult.getData();
        long now = timeMeter.currentTimeNanos();
        long consumedTokens = multiCommand.getConsumedTokens(multiResult);
        sampling.rememberRemoteCommandResult(consumedTokens, state.getRemoteStat().getConsumedTokens(), now);
    }


}
