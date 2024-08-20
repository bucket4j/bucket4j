package io.github.bucket4j.distributed.proxy.synchronization.batch;

import io.github.bucket4j.distributed.proxy.AsyncBackend;
import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.proxy.synchronization.Synchronization;
import io.github.bucket4j.distributed.proxy.synchronization.SynchronizationListener;

public class BatchingSynchronization implements Synchronization {

    @Override
    public <K> Backend<K> apply(Backend<K> backend, SynchronizationListener listener) {
        return new BatchingBackend<>(backend, listener);
    }

    @Override
    public <K> AsyncBackend<K> apply(AsyncBackend<K> backend, SynchronizationListener listener) {
        return new AsyncBatchingBackend<>(backend, listener);
    }

}
