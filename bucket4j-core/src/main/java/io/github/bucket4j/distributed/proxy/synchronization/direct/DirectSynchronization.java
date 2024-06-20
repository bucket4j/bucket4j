package io.github.bucket4j.distributed.proxy.synchronization.direct;

import io.github.bucket4j.distributed.proxy.AsyncBackend;
import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.proxy.synchronization.Synchronization;
import io.github.bucket4j.distributed.proxy.synchronization.SynchronizationListener;

public class DirectSynchronization implements Synchronization {

    public static final DirectSynchronization instance = new DirectSynchronization();

    @Override
    public <K> Backend<K> apply(Backend<K> backend, SynchronizationListener synchronizationListener) {
        return backend;
    }

    @Override
    public <K> AsyncBackend<K> apply(AsyncBackend<K> backend, SynchronizationListener synchronizationListener) {
        return backend;
    }

}
