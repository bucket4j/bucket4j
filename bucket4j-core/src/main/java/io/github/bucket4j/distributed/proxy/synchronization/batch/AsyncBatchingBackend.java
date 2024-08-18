package io.github.bucket4j.distributed.proxy.synchronization.batch;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.proxy.AsyncBackend;
import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronizationListener;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.batch.AsyncBatchingExecutor;
import io.github.bucket4j.distributed.proxy.synchronization.BucketSynchronizationListenerAdapter;
import io.github.bucket4j.distributed.proxy.synchronization.SynchronizationListener;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;

public class AsyncBatchingBackend<K> implements AsyncBackend<K> {

    private final AsyncBackend<K> target;
    private final ConcurrentHashMap<K, BatchingExecutorEntry> executors = new ConcurrentHashMap<>();
    private final SynchronizationListener synchronizationListener;
    private final BucketSynchronizationListener bucketSynchronizationListener;

    public AsyncBatchingBackend(AsyncBackend<K> target, SynchronizationListener synchronizationListener) {
        this.target = Objects.requireNonNull(target);
        this.synchronizationListener = synchronizationListener;
        this.bucketSynchronizationListener = new BucketSynchronizationListenerAdapter(synchronizationListener);
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> execute(K key, RemoteCommand<T> command) {
        BatchingExecutorEntry entry = executors.compute(key, (k, previous) -> {
            if (previous != null) {
                previous.inProgressCount++;
                return previous;
            } else {
                return new BatchingExecutorEntry(key, 1);
            }
        });
        AtomicBoolean decrementFlag = new AtomicBoolean();
        try {
            CompletableFuture<CommandResult<T>> future = entry.executor.executeAsync(command);
            future.whenComplete((result, throwable) -> tryDecrement(key, decrementFlag));
            return future;
        } catch (Throwable t) {
            tryDecrement(key, decrementFlag);
            throw BucketExceptions.from(t);
        }
    }

    private void tryDecrement(K key, AtomicBoolean decrementFlag) {
        if (decrementFlag.compareAndSet(false, true)) {
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
        private final AsyncBatchingExecutor executor;

        private BatchingExecutorEntry(K key, int inProgressCount) {
            this.inProgressCount = inProgressCount;
            AsyncCommandExecutor originalExecutor = new AsyncCommandExecutor() {
                @Override
                public <T> CompletableFuture<CommandResult<T>> executeAsync(RemoteCommand<T> command) {
                    return target.execute(key, command);
                }
            };
            executor = new AsyncBatchingExecutor(originalExecutor, bucketSynchronizationListener);
        }
    }

}
