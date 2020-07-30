package io.github.bucket4j.distributed.proxy.optimizers.predictive;

import io.github.bucket4j.RemoteVerboseResult;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.remote.commands.ConsumeIgnoringRateLimitsCommand;
import io.github.bucket4j.distributed.remote.commands.MultiCommand;
import io.github.bucket4j.distributed.remote.commands.VerboseCommand;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class PredictiveCommandExecutor implements CommandExecutor, AsyncCommandExecutor {

    private final CommandExecutor originalExecutor;
    private final AsyncCommandExecutor originalAsyncExecutor;
    private final PredictionParameters predictionParameters;
    private final TimeMeter timeMeter;

    private RemoteBucketState state;
    private long postponedToConsumeTokens;
    private long locallyConsumedByPredictionTokens;

    private Long lastSyncTimeNanos;
    private Long lastSyncConsumptionCounter;
    private Long lastSyncSelfConsumedTokens;
    private Long previousSyncTimeNanos;
    private Long previousSyncConsumptionCounter;

    PredictiveCommandExecutor(CommandExecutor originalExecutor, PredictionParameters predictionParameters, TimeMeter timeMeter) {
        this.originalExecutor = originalExecutor;
        this.originalAsyncExecutor = null;
        this.predictionParameters = predictionParameters;
        this.timeMeter = timeMeter;
    }

    PredictiveCommandExecutor(AsyncCommandExecutor originalAsyncExecutor, PredictionParameters predictionParameters, TimeMeter timeMeter) {
        this.originalExecutor = null;
        this.originalAsyncExecutor = originalAsyncExecutor;
        this.predictionParameters = predictionParameters;
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

        long predictedConsumptionByOthersSinceLastLocalCall = predictedConsumptionByOthers(currentTimeNanos);
        if (predictedConsumptionByOthersSinceLastLocalCall == Long.MAX_VALUE) {
            return null;
        }

        // prepare local command
        ConsumeIgnoringRateLimitsCommand consumeByPredictionCommand = new ConsumeIgnoringRateLimitsCommand(predictedConsumptionByOthersSinceLastLocalCall);
        List<RemoteCommand<?>> commands = Arrays.asList(consumeByPredictionCommand, command);
        MultiCommand multiCommand = new MultiCommand(commands);

        // execute local command
        PredictiveMutableEntry entry = new PredictiveMutableEntry(state.copy());
        MultiResult multiResult = multiCommand.execute(entry, currentTimeNanos).getData();
        long consumedTokens = CommandInsiders.getConsumedTokens(multiCommand, multiResult);
        if (consumedTokens == Long.MAX_VALUE) {
            return null;
        }

        long locallyConsumedTokens = consumedTokens - predictedConsumptionByOthersSinceLastLocalCall;
        if (!isLocalExecutionResultSatisfiesThreshold(locallyConsumedTokens)) {
            return null;
        }

        postponedToConsumeTokens += locallyConsumedTokens;
        locallyConsumedByPredictionTokens += predictedConsumptionByOthersSinceLastLocalCall;
        state = entry.getNewState();

        return (CommandResult<T>) multiResult.getResults().get(1);
    }

    private boolean isLocalExecutionResultSatisfiesThreshold(long locallyConsumedTokens) {
        return locallyConsumedTokens != Long.MAX_VALUE
                && postponedToConsumeTokens + locallyConsumedTokens >= 0
                && postponedToConsumeTokens + locallyConsumedTokens <= predictionParameters.getMaxUnsynchronizedTokens();
    }

    private <T> boolean isNeedToExecuteRemoteImmediately(RemoteCommand<T> command, long currentTimeNanos) {
        if (lastSyncTimeNanos == null || previousSyncTimeNanos == null ||
                currentTimeNanos - lastSyncTimeNanos > predictionParameters.getMaxUnsynchronizedTimeoutNanos() ||
                currentTimeNanos - previousSyncTimeNanos > predictionParameters.getMaxUnsynchronizedTimeoutNanos() * 2) {
            // need to execute immediately because lack of actual information about consumption rate in cluster
            return true;
        }

        if (CommandInsiders.isImmediateSyncRequired(command)) {
            // need to execute immediately because of special command
            return true;
        }

        long commandTokens = CommandInsiders.estimateTokensToConsume(command);
        if (commandTokens + postponedToConsumeTokens < 0) {
            // math overflow
            return true;
        }

        return commandTokens + postponedToConsumeTokens > predictionParameters.getMaxUnsynchronizedTokens();
    }

    private long predictedConsumptionByOthers(long currentTimeNanos) {
        if (!predictionParameters.shouldPredictConsumptionByOtherNodes()) {
            return 0L;
        }
        long timeSinceLastSync = currentTimeNanos - lastSyncTimeNanos;
        if (timeSinceLastSync <= 0) {
            return 0L;
        }

        long tokensConsumedByOthers = lastSyncConsumptionCounter - previousSyncConsumptionCounter - lastSyncSelfConsumedTokens;
        long previousIntervalBetweenSync = lastSyncTimeNanos - previousSyncTimeNanos;

        double othersRate = (double) tokensConsumedByOthers / (double) previousIntervalBetweenSync;
        double predictedConsumptionSinceLastSync = othersRate * (currentTimeNanos - timeSinceLastSync);
        if (predictedConsumptionSinceLastSync >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        long predictedComnsumptionSinceLastLocalCall = (long) (predictedConsumptionSinceLastSync - locallyConsumedByPredictionTokens);
        if (predictedComnsumptionSinceLastLocalCall < 0) {
            predictedComnsumptionSinceLastLocalCall = 0;
        }
        return predictedComnsumptionSinceLastLocalCall;
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
        // TODO
    }

}
