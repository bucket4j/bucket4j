package io.github.bucket4j.distributed.proxy.optimizers.batch.mock;

import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MultiResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.MultiCommand;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static io.github.bucket4j.serialization.PrimitiveSerializationHandles.*;

public class MockCommandExecutor implements CommandExecutor {

    private long sum;

    public long getSum() {
        return sum;
    }

    @Override
    public <T> CommandResult<T> execute(RemoteCommand<T> command) {
        if (command instanceof MultiCommand) {
            MultiCommand multiCommand = (MultiCommand) command;
            List<MockCommand> commands = (List) multiCommand.getCommands();
            List<CommandResult<Long>> results = new ArrayList<>(commands.size());
            for (MockCommand mockCommand : commands) {
                sum += mockCommand.getAmount();
                results.add(CommandResult.success(sum, LONG_HANDLE));
            }
            MultiResult multiResult = new MultiResult((List) results);
            return (CommandResult<T>) CommandResult.success(multiResult, MultiResult.SERIALIZATION_HANDLE);
        } else {
            MockCommand mockCommand = (MockCommand) command;
            sum += mockCommand.getAmount();
            return (CommandResult<T>) CommandResult.success(sum, LONG_HANDLE);
        }
    }

}
