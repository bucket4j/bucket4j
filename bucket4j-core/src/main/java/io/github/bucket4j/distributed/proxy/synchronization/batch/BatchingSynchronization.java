package io.github.bucket4j.distributed.proxy.synchronization.batch;

import io.github.bucket4j.distributed.proxy.AsyncBackend;
import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.proxy.synchronization.Synchronization;
import io.github.bucket4j.distributed.proxy.synchronization.batch.AsyncBatchingBackend;
import io.github.bucket4j.distributed.proxy.synchronization.batch.BatchingBackend;

public class BatchingSynchronization implements Synchronization {

    @Override
    public <K> Backend<K> apply(Backend<K> backend) {
        return new BatchingBackend<>(backend);
    }

    @Override
    public <K> AsyncBackend<K> apply(AsyncBackend<K> backend) {
        return new AsyncBatchingBackend<>(backend);
    }

}
