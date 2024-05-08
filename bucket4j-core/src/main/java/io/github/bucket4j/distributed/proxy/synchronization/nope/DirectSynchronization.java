package io.github.bucket4j.distributed.proxy.synchronization.nope;

import io.github.bucket4j.distributed.proxy.AsyncBackend;
import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.proxy.synchronization.Synchronization;

public class DirectSynchronization implements Synchronization {

    public static final DirectSynchronization instance = new DirectSynchronization();

    @Override
    public <K> Backend<K> apply(Backend<K> backend) {
        return backend;
    }

    @Override
    public <K> AsyncBackend<K> apply(AsyncBackend<K> backend) {
        return backend;
    }

}
