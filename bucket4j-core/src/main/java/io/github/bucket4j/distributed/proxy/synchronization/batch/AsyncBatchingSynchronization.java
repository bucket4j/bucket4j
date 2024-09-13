package io.github.bucket4j.distributed.proxy.synchronization.batch;

import io.github.bucket4j.distributed.proxy.AsyncBackend;
import io.github.bucket4j.distributed.proxy.synchronization.AsyncSynchronization;

public class AsyncBatchingSynchronization implements AsyncSynchronization {

    @Override
    public <K> AsyncBackend<K> apply(AsyncBackend<K> backend) {
        return new AsyncBatchingBackend<>(backend);
    }

}
