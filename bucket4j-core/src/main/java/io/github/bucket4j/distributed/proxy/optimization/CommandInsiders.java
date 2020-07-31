package io.github.bucket4j.distributed.proxy.optimization;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.remote.commands.*;
import io.github.bucket4j.serialization.SerializationHandle;

public class CommandInsiders {

    private static final CommandInsider[] insiders;

    public static <C extends RemoteCommand<?>> boolean isImmediateSyncRequired(C command) {
        return insiders[command.getSerializationHandle().getTypeId()].isImmediateSyncRequired(command);
    }

    public static <C extends RemoteCommand<?>> long estimateTokensToConsume(C command) {
        return insiders[command.getSerializationHandle().getTypeId()].estimateTokensToConsume(command);
    }

    public static <T, C extends RemoteCommand<T>> long getConsumedTokens(C command, T result) {
        return insiders[command.getSerializationHandle().getTypeId()].getConsumedTokens(command, result);
    }

    static {
        insiders = new CommandInsider[getMaxTypeId() + 1];

        register(ReserveAndCalculateTimeToSleepCommand.class, new CommandInsider<Long, ReserveAndCalculateTimeToSleepCommand>() {
            @Override
            public boolean isImmediateSyncRequired(ReserveAndCalculateTimeToSleepCommand command) {
                return false;
            }

            @Override
            public long estimateTokensToConsume(ReserveAndCalculateTimeToSleepCommand command) {
                return command.getTokensToConsume();
            }

            @Override
            public long getConsumedTokens(ReserveAndCalculateTimeToSleepCommand command, Long result) {
                return result;
            }
        });

        register(GetAvailableTokensCommand.class, new CommandInsider<Long, GetAvailableTokensCommand>() {
            @Override
            public boolean isImmediateSyncRequired(GetAvailableTokensCommand command) {
                return false;
            }

            @Override
            public long estimateTokensToConsume(GetAvailableTokensCommand command) {
                return 0;
            }

            @Override
            public long getConsumedTokens(GetAvailableTokensCommand command, Long result) {
                return 0;
            }
        });

        register(AddTokensCommand.class, new CommandInsider<Nothing, AddTokensCommand>() {
            @Override
            public boolean isImmediateSyncRequired(AddTokensCommand command) {
                return true;
            }

            @Override
            public long estimateTokensToConsume(AddTokensCommand command) {
                return 0;
            }

            @Override
            public long getConsumedTokens(AddTokensCommand command, Nothing result) {
                return 0;
            }
        });

        register(CreateInitialStateCommand.class, new CommandInsider<Nothing, CreateInitialStateCommand>() {
            @Override
            public boolean isImmediateSyncRequired(CreateInitialStateCommand command) {
                return true;
            }

            @Override
            public long estimateTokensToConsume(CreateInitialStateCommand command) {
                return 0;
            }

            @Override
            public long getConsumedTokens(CreateInitialStateCommand command, Nothing result) {
                return 0;
            }
        });

        register(ReplaceConfigurationOrReturnPreviousCommand.class, new CommandInsider<BucketConfiguration, ReplaceConfigurationOrReturnPreviousCommand>() {
            @Override
            public boolean isImmediateSyncRequired(ReplaceConfigurationOrReturnPreviousCommand command) {
                return true;
            }

            @Override
            public long estimateTokensToConsume(ReplaceConfigurationOrReturnPreviousCommand command) {
                return 0;
            }

            @Override
            public long getConsumedTokens(ReplaceConfigurationOrReturnPreviousCommand command, BucketConfiguration result) {
                return 0;
            }
        });

        register(TryConsumeCommand.class, new CommandInsider<Boolean, TryConsumeCommand>() {
            @Override
            public boolean isImmediateSyncRequired(TryConsumeCommand command) {
                return false;
            }

            @Override
            public long estimateTokensToConsume(TryConsumeCommand command) {
                return command.getTokensToConsume();
            }

            @Override
            public long getConsumedTokens(TryConsumeCommand command, Boolean result) {
                return result ? command.getTokensToConsume() : 0;
            }
        });

        register(ConsumeIgnoringRateLimitsCommand.class, new CommandInsider<Long, ConsumeIgnoringRateLimitsCommand>() {
            @Override
            public boolean isImmediateSyncRequired(ConsumeIgnoringRateLimitsCommand command) {
                return false;
            }

            @Override
            public long estimateTokensToConsume(ConsumeIgnoringRateLimitsCommand command) {
                return command.getTokensToConsume();
            }

            @Override
            public long getConsumedTokens(ConsumeIgnoringRateLimitsCommand command, Long result) {
                return result == Long.MAX_VALUE? 0l: command.getTokensToConsume();
            }
        });

        register(GetConfigurationCommand.class, new CommandInsider<BucketConfiguration, GetConfigurationCommand>() {
            @Override
            public boolean isImmediateSyncRequired(GetConfigurationCommand command) {
                return false;
            }

            @Override
            public long estimateTokensToConsume(GetConfigurationCommand command) {
                return 0;
            }

            @Override
            public long getConsumedTokens(GetConfigurationCommand command, BucketConfiguration result) {
                return 0;
            }
        });

        register(VerboseCommand.class, cast(new CommandInsider<RemoteVerboseResult<Object>, VerboseCommand<Object>>() {
            @Override
            public boolean isImmediateSyncRequired(VerboseCommand command) {
                return CommandInsiders.isImmediateSyncRequired(command.getTargetCommand());
            }

            @Override
            public long estimateTokensToConsume(VerboseCommand command) {
                return CommandInsiders.estimateTokensToConsume(command.getTargetCommand());
            }

            @Override
            public long getConsumedTokens(VerboseCommand<Object> command, RemoteVerboseResult<Object> result1) {
                return CommandInsiders.getConsumedTokens(command.getTargetCommand(), result1.getValue());
            }

        }));

        register(CreateSnapshotCommand.class, new CommandInsider<RemoteBucketState, CreateSnapshotCommand>() {
            @Override
            public boolean isImmediateSyncRequired(CreateSnapshotCommand command) {
                return false;
            }

            @Override
            public long estimateTokensToConsume(CreateSnapshotCommand command) {
                return 0;
            }

            @Override
            public long getConsumedTokens(CreateSnapshotCommand command, RemoteBucketState result) {
                return 0;
            }
        });

        register(MultiCommand.class, new CommandInsider<MultiResult, MultiCommand>() {
            @Override
            public boolean isImmediateSyncRequired(MultiCommand multiCommand) {
                for (RemoteCommand<?> command : multiCommand.getCommands()) {
                    if (CommandInsiders.isImmediateSyncRequired(command)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public long estimateTokensToConsume(MultiCommand multiCommand) {
                long sum = 0;
                for (RemoteCommand<?> command : multiCommand.getCommands()) {
                    sum += CommandInsiders.estimateTokensToConsume(command);
                    if (sum < 0l) {
                        // math overflow
                        return Long.MAX_VALUE;
                    }
                }
                return sum;
            }

            @Override
            public long getConsumedTokens(MultiCommand multiCommand, MultiResult multiResult) {
                long sum = 0;
                int count = multiCommand.getCommands().size();
                for (int i = 0; i < count; i++) {
                    RemoteCommand command = multiCommand.getCommands().get(i);
                    CommandResult result = multiResult.getResults().get(i);
                    sum += CommandInsiders.getConsumedTokens(command, result);
                    if (sum < 0l) {
                        // math overflow
                        return Long.MAX_VALUE;
                    }
                }
                return sum;
            }
        });

        register(CreateInitialStateAndExecuteCommand.class, cast(new CommandInsider<Object, CreateInitialStateAndExecuteCommand<Object>>() {
            @Override
            public boolean isImmediateSyncRequired(CreateInitialStateAndExecuteCommand command) {
                return true;
            }

            @Override
            public long estimateTokensToConsume(CreateInitialStateAndExecuteCommand command) {
                return CommandInsiders.estimateTokensToConsume(command.getTargetCommand());
            }

            @Override
            public long getConsumedTokens(CreateInitialStateAndExecuteCommand command, Object result1) {
                return CommandInsiders.getConsumedTokens(command.getTargetCommand(), result1);
            }
        }));

        register(EstimateAbilityToConsumeCommand.class, new CommandInsider<EstimationProbe, EstimateAbilityToConsumeCommand>() {
            @Override
            public boolean isImmediateSyncRequired(EstimateAbilityToConsumeCommand command) {
                return false;
            }

            @Override
            public long estimateTokensToConsume(EstimateAbilityToConsumeCommand command) {
                return 0;
            }

            @Override
            public long getConsumedTokens(EstimateAbilityToConsumeCommand command, EstimationProbe result) {
                return 0;
            }
        });

        register(TryConsumeAndReturnRemainingTokensCommand.class, new CommandInsider<ConsumptionProbe, TryConsumeAndReturnRemainingTokensCommand>() {
            @Override
            public boolean isImmediateSyncRequired(TryConsumeAndReturnRemainingTokensCommand command) {
                return false;
            }

            @Override
            public long estimateTokensToConsume(TryConsumeAndReturnRemainingTokensCommand command) {
                return command.getTokensToConsume();
            }

            @Override
            public long getConsumedTokens(TryConsumeAndReturnRemainingTokensCommand command, ConsumptionProbe result) {
                return result.isConsumed() ? command.getTokensToConsume() : 0L;
            }
        });

        register(ConsumeAsMuchAsPossibleCommand.class, new CommandInsider<Long, ConsumeAsMuchAsPossibleCommand>() {
            @Override
            public boolean isImmediateSyncRequired(ConsumeAsMuchAsPossibleCommand command) {
                return command.getLimit() == Long.MAX_VALUE;
            }

            @Override
            public long estimateTokensToConsume(ConsumeAsMuchAsPossibleCommand command) {
                return command.getLimit();
            }

            @Override
            public long getConsumedTokens(ConsumeAsMuchAsPossibleCommand command, Long result) {
                return result;
            }
        });

        register(SyncCommand.class, new CommandInsider<Nothing, SyncCommand>() {
            @Override
            public boolean isImmediateSyncRequired(SyncCommand command) {
                return true;
            }

            @Override
            public long estimateTokensToConsume(SyncCommand command) {
                return 0L;
            }

            @Override
            public long getConsumedTokens(SyncCommand command, Nothing result) {
                return 0L;
            }
        });

        // check that all insiders were registered
        checkThatAllInsidersWereRegistered();
    }

    private static <T, C extends RemoteCommand<T>, K extends Class<C>> CommandInsider<T, C> cast(CommandInsider<?, ?> insider) {
        return (CommandInsider<T, C>) insider;
    }

    private static void checkThatAllInsidersWereRegistered() {
        SerializationHandle notRegistered = SerializationHandle.CORE_HANDLES
                .getAllHandles().stream()
                .filter(handler -> handler.getSerializedType().isAssignableFrom(RemoteCommand.class))
                .filter(handler -> insiders[handler.getTypeId()] == null)
                .findFirst().orElse(null);
        if (notRegistered != null) {
            throw new IllegalStateException("Insider is not registered for " + notRegistered.getSerializedType());
        }
    }

    private static  <T, C extends RemoteCommand<T>, K extends Class<C>> void register(K klass, CommandInsider<T, C> insider) {
        int index = SerializationHandle.CORE_HANDLES
                .getAllHandles().stream()
                .filter(handler -> handler.getSerializedType().equals(klass))
                .mapToInt(handler -> handler.getTypeId())
                .findFirst()
                .orElse(-1);
        insiders[index] = insider;
    }

    private static int getMaxTypeId() {
        return SerializationHandle.CORE_HANDLES
                .getAllHandles().stream()
                .filter(handler -> RemoteCommand.class.isAssignableFrom(handler.getSerializedType()))
                .mapToInt(handler -> handler.getTypeId())
                .max().orElse(-1);
    }

    private interface CommandInsider<T, C extends RemoteCommand<T>> {

        boolean isImmediateSyncRequired(C command);

        long estimateTokensToConsume(C command);

        long getConsumedTokens(C command, T result);

    }

    public static void main(String[] args) {
        System.out.println(insiders.length);
    }

}
