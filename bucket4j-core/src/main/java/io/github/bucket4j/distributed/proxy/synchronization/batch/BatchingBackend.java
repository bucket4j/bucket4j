package io.github.bucket4j.distributed.proxy.synchronization.batch;

import java.util.concurrent.ConcurrentHashMap;

import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.optimization.NopeOptimizationListener;
import io.github.bucket4j.distributed.proxy.optimization.batch.BatchingExecutor;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;

public class BatchingBackend<K> implements Backend<K> {

    private final Backend<K> target;
    private final ConcurrentHashMap<K, BatchingExecutorEntry> executors = new ConcurrentHashMap<>();

    public BatchingBackend(Backend<K> target) {
        this.target = target;
    }

    @Override
    public <T> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        BatchingExecutorEntry entry = executors.compute(key, (k, previous) -> {
            if (previous != null) {
                previous.inProgressCount++;
                return previous;
            } else {
                return new BatchingExecutorEntry(key, 1);
            }
        });
        try {
            return entry.executor.execute(command);
        } finally {
            executors.compute(key, (k, previous) -> {
                if (previous == null) {
                    // should not be there
                    return null;
                } else {
                    previous.inProgressCount--;
                    return previous.inProgressCount == 0 ? null : previous;
                }
            });
        }
    }

    private final class BatchingExecutorEntry {

        private int inProgressCount;
        private final BatchingExecutor executor;

        private BatchingExecutorEntry(K key, int inProgressCount) {
            this.inProgressCount = inProgressCount;

            CommandExecutor originalExecutor = new CommandExecutor() {
                @Override
                public <T> CommandResult<T> execute(RemoteCommand<T> command) {
                    return target.execute(key, command);
                }
            };
            executor = new BatchingExecutor(originalExecutor, NopeOptimizationListener.INSTANCE);
        }
    }

}
