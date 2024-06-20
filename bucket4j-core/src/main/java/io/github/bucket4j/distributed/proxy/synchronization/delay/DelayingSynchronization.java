package io.github.bucket4j.distributed.proxy.synchronization.delay;

import io.github.bucket4j.distributed.proxy.AsyncBackend;
import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.proxy.synchronization.Synchronization;
import io.github.bucket4j.distributed.proxy.synchronization.SynchronizationListener;
import io.github.bucket4j.distributed.proxy.synchronization.batch.AsyncBatchingBackend;
import io.github.bucket4j.distributed.proxy.synchronization.batch.BatchingBackend;

public class DelayingSynchronization implements Synchronization {

    @Override
    public <K> Backend<K> apply(Backend<K> backend, SynchronizationListener synchronizationListener) {
        // TODO
        return new BatchingBackend<>(backend, synchronizationListener);
    }

    @Override
    public <K> AsyncBackend<K> apply(AsyncBackend<K> backend, SynchronizationListener synchronizationListener) {
        // TODO
        return new AsyncBatchingBackend<>(backend, synchronizationListener);
    }

}
