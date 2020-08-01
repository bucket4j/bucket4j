package io.github.bucket4j.distributed.proxy.optimization.predictive;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.optimization.*;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.remote.commands.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class PredictiveCommandExecutor implements CommandExecutor, AsyncCommandExecutor {

    private static final int ORIGINAL_COMMAND_INDEX = 1;
    private static final int GET_SNAPSHOT_COMMAND_INDEX = 2;

    private final CommandExecutor originalExecutor;
    private final AsyncCommandExecutor originalAsyncExecutor;
    private final PredictionParameters predictionParameters;
    private final DelayParameters delayParameters;
    private final OptimizationListener listener;
    private final TimeMeter timeMeter;


    private RemoteBucketState state;
    private long postponedToConsumeTokens;
    private long consumedByPredictionTokens;

    private LinkedList<Sample> samples = new LinkedList<>();

    PredictiveCommandExecutor(CommandExecutor originalExecutor, DelayParameters delayParameters,
                              PredictionParameters predictionParameters, OptimizationListener listener,
                              TimeMeter timeMeter) {
        this.originalExecutor = originalExecutor;
        this.originalAsyncExecutor = null;
        this.predictionParameters = predictionParameters;
        this.delayParameters = delayParameters;
        this.listener = listener;
        this.timeMeter = timeMeter;
    }

    PredictiveCommandExecutor(AsyncCommandExecutor originalAsyncExecutor, DelayParameters delayParameters,
                              PredictionParameters predictionParameters, OptimizationListener listener,
                              TimeMeter timeMeter) {
        this.originalExecutor = null;
        this.originalAsyncExecutor = originalAsyncExecutor;
        this.predictionParameters = predictionParameters;
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
        if (results.get(GET_SNAPSHOT_COMMAND_INDEX).isBucketNotFound()) {
            return (CommandResult<T>) results.get(ORIGINAL_COMMAND_INDEX);
        }
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
            if (results.get(GET_SNAPSHOT_COMMAND_INDEX).isBucketNotFound()) {
                return (CommandResult<T>) results.get(ORIGINAL_COMMAND_INDEX);
            }
            rememberRemoteCommandResult(remoteCommand, multiResult);
            return (CommandResult<T>) results.get(ORIGINAL_COMMAND_INDEX);
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
        BucketEntryWrapper entry = new BucketEntryWrapper(state.copy());
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
        consumedByPredictionTokens += predictedConsumptionByOthersSinceLastLocalCall;
        if (entry.isStateModified()) {
            state = entry.get();
        }

        return (CommandResult<T>) multiResult.getResults().get(1);
    }

    private boolean isLocalExecutionResultSatisfiesThreshold(long locallyConsumedTokens) {
        if (locallyConsumedTokens == Long.MAX_VALUE || postponedToConsumeTokens + locallyConsumedTokens < 0) {
            // math overflow
            return false;
        }
        return postponedToConsumeTokens + locallyConsumedTokens <= delayParameters.maxUnsynchronizedTokens;
    }

    private <T> boolean isNeedToExecuteRemoteImmediately(RemoteCommand<T> command, long currentTimeNanos) {
        if (samples.size() < predictionParameters.requiredSamples) {
            // there is no enough samples to predict rate
            return true;
        }

        if (currentTimeNanos - samples.getLast().syncTimeNanos  > delayParameters.maxUnsynchronizedTimeoutNanos) {
            // too long period passed since last sync
            return true;
        }

        for (Sample sample : samples) {
            if (currentTimeNanos - sample.syncTimeNanos > predictionParameters.sampleMaxAgeNanos) {
                // there is no enough samples to predict rate
                return true;
            }
        }

        if (CommandInsiders.isImmediateSyncRequired(command, postponedToConsumeTokens, currentTimeNanos - samples.getLast().syncTimeNanos)) {
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

    private long predictedConsumptionByOthers(long currentTimeNanos) {
        Sample freshSample = samples.getLast();
        long lastSyncTimeNanos = freshSample.syncTimeNanos;
        long timeSinceLastSync = currentTimeNanos - lastSyncTimeNanos;
        if (timeSinceLastSync <= 0) {
            return 0L;
        }

        long lastObservedConsumptionCounter = freshSample.observedConsumptionCounter;
        Sample oldestSample = samples.getFirst();
        long oldestSyncConsumptionCounter = oldestSample.observedConsumptionCounter;
        long oldestSyncTimeNanos = oldestSample.syncTimeNanos;

        long tokensSelfConsumedDuringSamplePeriod = 0;
        for (Sample sample : samples) {
            if (sample != oldestSample) {
                tokensSelfConsumedDuringSamplePeriod += sample.selfConsumedTokens;
            }
        }

        long tokensConsumedByOthersDuringSamplingPeriod = lastObservedConsumptionCounter - oldestSyncConsumptionCounter - tokensSelfConsumedDuringSamplePeriod;
        long previousIntervalBetweenSync = lastSyncTimeNanos - oldestSyncTimeNanos;

        double othersRate = (double) tokensConsumedByOthersDuringSamplingPeriod / (double) previousIntervalBetweenSync;
        double predictedConsumptionSinceLastSync = othersRate * (currentTimeNanos - timeSinceLastSync);
        if (predictedConsumptionSinceLastSync >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        long predictedComnsumptionSinceLastLocalCall = (long) (predictedConsumptionSinceLastSync - consumedByPredictionTokens);
        if (predictedComnsumptionSinceLastLocalCall < 0) {
            predictedComnsumptionSinceLastLocalCall = 0;
        }
        return predictedComnsumptionSinceLastLocalCall;
    }

    private <T> MultiCommand prepareRemoteCommand(RemoteCommand<T> command) {
        List<RemoteCommand<?>> commands = new ArrayList<>(3);
        commands.add(new ConsumeIgnoringRateLimitsCommand(this.postponedToConsumeTokens));
        commands.add(command);
        commands.add(new CreateSnapshotCommand());
        return new MultiCommand(commands);
    }

    private void rememberRemoteCommandResult(MultiCommand remoteCommand, MultiResult remoteResult) {
        state = (RemoteBucketState) remoteResult.getResults().get(GET_SNAPSHOT_COMMAND_INDEX).getData();
        postponedToConsumeTokens = 0;
        consumedByPredictionTokens = 0;
        Sample sample = new Sample(
                timeMeter.currentTimeNanos(),
                state.getRemoteStat().getConsumedTokens(),
                CommandInsiders.getConsumedTokens(remoteCommand, remoteResult)
        );
        samples.addLast(sample);
        if (samples.size() > predictionParameters.requiredSamples) {
            samples.removeFirst();
        }
    }

    private static class Sample {
        private long syncTimeNanos;
        private long observedConsumptionCounter;
        private long selfConsumedTokens;

        public Sample(long syncTimeNanos, long observedConsumptionCounter, long selfConsumedTokens) {
            this.syncTimeNanos = syncTimeNanos;
            this.observedConsumptionCounter = observedConsumptionCounter;
            this.selfConsumedTokens = selfConsumedTokens;
        }
    }


}
