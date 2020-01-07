package io.github.bucket4j.distributed.proxy.optimizers.batch;

import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.MultiCommand;
import io.github.bucket4j.distributed.remote.MultiResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.github.bucket4j.serialization.PrimitiveSerializationHandles.LONG_HANDLE;

public class BatchingExecutor implements CommandExecutor {

    private static final CommandResult SIGNAL_TO_SCHEDULE_NEXT_BATCH = CommandResult.success(42L, LONG_HANDLE);

    private final CommandExecutor wrappedExecutor;

    private final State state = new State();

    public BatchingExecutor(CommandExecutor originalExecutor) {
        this.wrappedExecutor = originalExecutor;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(RemoteCommand<T> command) {
        Thread currentThread = Thread.currentThread();
        WaitingNode waitingNode = null;

        synchronized (state) {
            if (state.exclusiveOwner == null) {
                state.exclusiveOwner = currentThread;
            } else {
                waitingNode = new WaitingNode(command);
                state.waitingCommands.add(waitingNode);
            }
        }

        if (waitingNode == null) {
            try {
                return wrappedExecutor.execute(command);
            } finally {
                wakeupFirstThreadFromNextBatch();
            }
        }

        CommandResult result = waitingNode.waitUninterruptedly();
        if (result != SIGNAL_TO_SCHEDULE_NEXT_BATCH) {
            // our future completed by another thread from current batch
            return result;
        }

        // current thread is responsible to execute the batch of commands
        try {
            return executeBatch();
        } finally {
            wakeupFirstThreadFromNextBatch();
        }

    }

    private <T extends Serializable> CommandResult<T> executeBatch() {
        List<WaitingNode> waitingNodes;
        synchronized (state) {
            waitingNodes = state.waitingCommands;
            state.waitingCommands = new ArrayList<>();
        }

        if (waitingNodes.size() == 1) {
            RemoteCommand<?> singleCommand = waitingNodes.get(0).command;
            return (CommandResult<T>) wrappedExecutor.execute(singleCommand);
        }

        try {
            List<RemoteCommand<?>> commandsInBatch = new ArrayList<>(waitingNodes.size());
            for (WaitingNode waitingNode : waitingNodes) {
                commandsInBatch.add(waitingNode.command);
            }
            MultiCommand multiCommand = new MultiCommand(commandsInBatch);

            CommandResult<MultiResult> multiResult = wrappedExecutor.execute(multiCommand);
            List<CommandResult<?>> singleResults = multiResult.getData().getResults();
            for (int i = 0; i < waitingNodes.size(); i++) {
                waitingNodes.get(i).future.complete(singleResults.get(i));
            }

            return (CommandResult<T>) singleResults.get(0);
        } catch (Throwable e) {
            for (WaitingNode waitingNode : waitingNodes) {
                waitingNode.future.completeExceptionally(e);
            }
            throw new BatchFailedException(e);
        }
    }

    private void wakeupFirstThreadFromNextBatch() {
        synchronized (state) {
            if (state.waitingCommands.isEmpty()) {
                state.exclusiveOwner = null;
            } else {
                WaitingNode firstWaitingNode = state.waitingCommands.get(0);
                state.exclusiveOwner = firstWaitingNode.waitingThread;
                firstWaitingNode.future.complete(SIGNAL_TO_SCHEDULE_NEXT_BATCH);
            }
        }
    }

    private static class State {

        private List<WaitingNode> waitingCommands = new ArrayList<>();

        private Thread exclusiveOwner;

    }

    private class WaitingNode {

        private final Thread waitingThread = Thread.currentThread();
        private final RemoteCommand<?> command;
        private final CompletableFuture<CommandResult> future = new CompletableFuture<>();

        private WaitingNode(RemoteCommand<?> command) {
            this.command = command;
        }

        public CommandResult waitUninterruptedly() {
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

}
