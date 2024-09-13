package io.github.bucket4j.distributed.proxy.synchronization.direct;

import io.github.bucket4j.distributed.proxy.AsyncBackend;
import io.github.bucket4j.distributed.proxy.synchronization.AsyncSynchronization;

public class AsyncDirectSynchronization implements AsyncSynchronization {

    public static final AsyncDirectSynchronization instance = new AsyncDirectSynchronization();

    @Override
    public <K> AsyncBackend<K> apply(AsyncBackend<K> backend) {
        return backend;
    }

}
