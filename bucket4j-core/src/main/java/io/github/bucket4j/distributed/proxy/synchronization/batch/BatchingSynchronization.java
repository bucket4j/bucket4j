package io.github.bucket4j.distributed.proxy.synchronization.batch;

import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.proxy.synchronization.Synchronization;

public class BatchingSynchronization implements Synchronization {

    @Override
    public <K> Backend<K> apply(Backend<K> backend) {
        return new BatchingBackend<>(backend);
    }

}
