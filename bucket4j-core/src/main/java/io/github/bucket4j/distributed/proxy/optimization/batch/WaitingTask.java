
package io.github.bucket4j.distributed.proxy.optimization.batch;

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.github.bucket4j.serialization.PrimitiveSerializationHandles.LONG_HANDLE;

public class WaitingTask {

    public static final CommandResult<?> NEED_TO_EXECUTE_NEXT_BATCH = CommandResult.success(42L, LONG_HANDLE);

    public final RemoteCommand<?> command;
    public final CompletableFuture<CommandResult<?>> future = new CompletableFuture<>();

    public WaitingTask previous;

    WaitingTask(RemoteCommand<?> command) {
        this.command = command;
    }

    public CommandResult<?> waitUninterruptedly() {
        boolean wasInterrupted = false;;
        try {
            while (true) {
                wasInterrupted = wasInterrupted || Thread.interrupted();
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    wasInterrupted = true;
                } catch (ExecutionException e) {
                    throw new BatchFailedException(e.getCause());
                }
            }
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
