package io.github.bucket4j.distributed.proxy.synchronization.delay;

import java.util.concurrent.ConcurrentHashMap;

import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.NopeOptimizationListener;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.batch.BatchingExecutor;
import io.github.bucket4j.distributed.proxy.synchronization.SynchronizationListener;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;

public class DelayingBackend<K> implements Backend<K> {

    private final Backend<K> target;
    private final ConcurrentHashMap<K, DelayingExecutorEntry> executors = new ConcurrentHashMap<>();
    private final SynchronizationListener synchronizationListener;

    public DelayingBackend(Backend<K> target, SynchronizationListener synchronizationListener) {
        this.target = target;
        this.synchronizationListener = synchronizationListener;
    }

    @Override
    public <T> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        DelayingExecutorEntry entry = executors.compute(key, (k, previous) -> {
            if (previous != null) {
                previous.inProgressCount++;
                return previous;
            } else {
                return new DelayingExecutorEntry(key, 1);
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

    private final class DelayingExecutorEntry {

        private int inProgressCount;
        private final BatchingExecutor executor;

        private DelayingExecutorEntry(K key, int inProgressCount) {
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
