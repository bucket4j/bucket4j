package io.github.bucket4j.distributed.proxy.synchronization.delay;

import io.github.bucket4j.distributed.proxy.AsyncBackend;
import io.github.bucket4j.distributed.proxy.synchronization.AsyncSynchronization;
import io.github.bucket4j.distributed.proxy.synchronization.batch.AsyncBatchingBackend;

public class AsyncDelayingSynchronization implements AsyncSynchronization {

    @Override
    public <K> AsyncBackend<K> apply(AsyncBackend<K> backend) {
        return new AsyncBatchingBackend<>(backend);
    }

}
