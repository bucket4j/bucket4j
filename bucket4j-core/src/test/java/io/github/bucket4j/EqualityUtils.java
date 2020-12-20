package io.github.bucket4j;

import io.github.bucket4j.grid.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;


public class EqualityUtils {

    private static final Map<Class<?>, BiFunction<?, ?, Boolean>> comparators = new HashMap<>();
    static {
        registerComparator(Bandwidth.class, (bandwidth1, bandwidth2) ->
                bandwidth1.capacity == bandwidth2.capacity &&
                bandwidth1.initialTokens == bandwidth2.initialTokens &&
                bandwidth1.refillPeriodNanos == bandwidth2.refillPeriodNanos &&
                bandwidth1.refillTokens == bandwidth2.refillTokens &&
                bandwidth1.refillIntervally == bandwidth2.refillIntervally &&
                bandwidth1.timeOfFirstRefillMillis == bandwidth2.timeOfFirstRefillMillis &&
                bandwidth1.useAdaptiveInitialTokens == bandwidth2.useAdaptiveInitialTokens &&
                Objects.equals(bandwidth1.id, bandwidth2.id)
        );

        registerComparator(BucketConfiguration.class, (config1, config2) -> {
                if (config1.getBandwidths().length != config2.getBandwidths().length) {
                    return false;
                }
                for (int i = 0; i < config1.getBandwidths().length; i++) {
                    Bandwidth bandwidth1 = config1.getBandwidths()[i];
                    Bandwidth bandwidth2 = config2.getBandwidths()[i];
                    if (!equals(bandwidth1, bandwidth2)) {
                        return false;
                    }
                }
                return true;
            }
        );

        registerComparator(CommandResult.class, (result1, result2) -> {
            return result1.isBucketNotFound() == result2.isBucketNotFound() &&
                    equals(result1.getData(), result2.getData());
        });

        registerComparator(GridBucketState.class, (state1, state2) -> {
            return equals(state1.getConfiguration(), state2.getConfiguration()) &&
                    equals(state1.getState(), state2.getState());
        });

        registerComparator(GridBucketState.class, (state1, state2) -> {
            return equals(state1.getConfiguration(), state2.getConfiguration()) &&
                    equals(state1.getState(), state2.getState());
        });

        registerComparator(EstimationProbe.class, (probe1, probe2) -> {
            return probe1.canBeConsumed() == probe2.canBeConsumed() &&
                    probe1.getNanosToWaitForRefill() == probe2.getNanosToWaitForRefill() &&
                    probe1.getRemainingTokens() == probe2.getRemainingTokens();
        });

        registerComparator(ConsumptionProbe.class, (probe1, probe2) -> {
            return probe1.isConsumed() == probe2.isConsumed() &&
                    probe1.getNanosToWaitForRefill() == probe2.getNanosToWaitForRefill() &&
                    probe1.getRemainingTokens() == probe2.getRemainingTokens();
        });

        registerComparator(VerboseResult.class, (result1, result2) -> {
            return result1.getOperationTimeNanos() == result2.getOperationTimeNanos() &&
                    equals(result1.getValue(), result2.getValue()) &&
                    equals(result1.getConfiguration(), result2.getConfiguration()) &&
                    equals(result1.getState(), result2.getState());
        });

        registerComparator(ReserveAndCalculateTimeToSleepCommand.class, (cmd1, cmd2) -> {
            return cmd1.getTokensToConsume() == cmd2.getTokensToConsume() &&
                    cmd1.getWaitIfBusyNanosLimit() == cmd2.getWaitIfBusyNanosLimit();
        });

        registerComparator(AddTokensCommand.class, (cmd1, cmd2) -> {
            return cmd1.getTokensToAdd() == cmd2.getTokensToAdd();
        });

        registerComparator(ConsumeAsMuchAsPossibleCommand.class, (cmd1, cmd2) -> {
            return cmd1.getLimit() == cmd2.getLimit();
        });

        registerComparator(CreateSnapshotCommand.class, (cmd1, cmd2) -> {
            return true;
        });

        registerComparator(GetAvailableTokensCommand.class, (cmd1, cmd2) -> {
            return true;
        });

        registerComparator(EstimateAbilityToConsumeCommand.class, (cmd1, cmd2) -> {
            return cmd1.getTokensToConsume() == cmd2.getTokensToConsume();
        });

        registerComparator(TryConsumeCommand.class, (cmd1, cmd2) -> {
            return cmd1.getTokensToConsume() == cmd2.getTokensToConsume();
        });

        registerComparator(TryConsumeAndReturnRemainingTokensCommand.class, (cmd1, cmd2) -> {
            return cmd1.getTokensToConsume() == cmd2.getTokensToConsume();
        });

        registerComparator(ReplaceConfigurationCommand.class, (cmd1, cmd2) -> {
            return equals(cmd1.getNewConfiguration(), cmd2.getNewConfiguration()) &&
                    Objects.equals(cmd1.getTokensMigrationMode(), cmd2.getTokensMigrationMode());
        });

        registerComparator(ConsumeIgnoringRateLimitsCommand.class, (cmd1, cmd2) -> {
            return equals(cmd1.getTokensToConsume(), cmd2.getTokensToConsume());
        });

        registerComparator(VerboseCommand.class, (cmd1, cmd2) -> {
            return equals(cmd1.getTargetCommand(), cmd2.getTargetCommand());
        });
    }

    public static <T> void registerComparator(Class<T> clazz, BiFunction<T, T, Boolean> comparator) {
        comparators.put(clazz, comparator);
    }

    public static <T> boolean equals(T object1, T object2) {
        if (object1 == object2) {
            return true;
        }
        if ((object1 == null && object2 != null) || (object1 != null && object2 == null)) {
            return false;
        }
        BiFunction<T, T, Boolean> comparator = (BiFunction<T, T, Boolean>) comparators.get(object1.getClass());
        if (comparator == null) {
            return object1.equals(object2);
        }
        return comparator.apply(object1, object2);
    }

}
