package io.github.bucket4j.distributed.proxy.optimizers.batch.mock;

import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MultiResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.MultiCommand;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static io.github.bucket4j.serialization.PrimitiveSerializationHandles.LONG_HANDLE;

public class MockAsyncCommandExecutor implements AsyncCommandExecutor {

    static Executor executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }
    });

    private long sum;

    public long getSum() {
        return sum;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(RemoteCommand<T> command) {
        return CompletableFuture.supplyAsync(() -> this.executeSync(command), executor);
    }

    @Override
    public CompletableFuture<Void> flushAsync() {
        return CompletableFuture.completedFuture(null);
    }

    private <T> CommandResult<T> executeSync(RemoteCommand<T> command) {
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
            return (CommandResult) CommandResult.success(sum, LONG_HANDLE);
        }
    }

}
