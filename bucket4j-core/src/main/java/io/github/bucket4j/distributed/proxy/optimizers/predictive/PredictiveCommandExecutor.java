package io.github.bucket4j.distributed.proxy.optimizers.predictive;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.VerboseResult;
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
    private final PredictionThresholds thresholds;
    private final TimeMeter timeMeter;

    private RemoteBucketState state;
    private long postponedToConsumeTokens;
    private long locallyConsumedByPredictionTokens;
    private Long lastSyncTimeNanos;
    private Long lastSyncConsumptionCounter;
    private Long lastSyncSelfConsumedTokens;
    private Long previousSyncTimeNanos;
    private Long previousSyncConsumptionCounter;

    PredictiveCommandExecutor(CommandExecutor originalExecutor, PredictionThresholds thresholds, TimeMeter timeMeter) {
        this.originalExecutor = originalExecutor;
        this.originalAsyncExecutor = null;
        this.thresholds = thresholds;
        this.timeMeter = timeMeter;
    }

    PredictiveCommandExecutor(AsyncCommandExecutor originalAsyncExecutor, PredictionThresholds thresholds, TimeMeter timeMeter) {
        this.originalExecutor = null;
        this.originalAsyncExecutor = originalAsyncExecutor;
        this.thresholds = thresholds;
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
        VerboseResult<MultiResult> remoteResult = executeRemote(remoteCommand);


        return null;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(RemoteCommand<T> command) {
        CommandResult<T> result = tryConsumeLocally(command);
        if (result != null) {
            // remote call is not needed
            return CompletableFuture.completedFuture(result);
        }
        return null;
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
                && postponedToConsumeTokens + locallyConsumedTokens <= thresholds.getMaxUnsynchronizedTokens();
    }

    private <T> boolean isNeedToExecuteRemoteImmediately(RemoteCommand<T> command, long currentTimeNanos) {
        if (lastSyncTimeNanos == null || previousSyncTimeNanos == null ||
                currentTimeNanos - lastSyncTimeNanos > thresholds.getMaxUnsynchronizedTimeoutNanos() ||
                currentTimeNanos - previousSyncTimeNanos > thresholds.getMaxUnsynchronizedTimeoutNanos() * 2) {
            // need to execute immediately because lack of actual information about consumption rate in cluster
            return true;
        }

        if (CommandInsiders.isMutationNotRelatedWithConsumption(command)) {
            // need to execute immediately because of special command
            return true;
        }

        long commandTokens = CommandInsiders.estimateTokensToConsume(command);
        if (commandTokens + postponedToConsumeTokens < 0) {
            // math overflow
            return true;
        }

        return commandTokens + postponedToConsumeTokens > thresholds.getMaxUnsynchronizedTokens();
    }

    private long predictedConsumptionByOthers(long currentTimeNanos) {
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

    private VerboseResult<MultiResult> executeRemote(VerboseCommand<MultiResult> remoteCommand) {
        // TODO
        return null;
    }

    private <T> VerboseCommand<MultiResult> prepareRemoteCommand(RemoteCommand<T> command) {
        // TODO
        return null;
    }

}
