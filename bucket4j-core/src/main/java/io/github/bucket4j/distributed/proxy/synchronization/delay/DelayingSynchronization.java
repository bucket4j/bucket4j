package io.github.bucket4j.distributed.proxy.synchronization.delay;

import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.proxy.synchronization.Synchronization;

public class DelayingSynchronization implements Synchronization {

    @Override
    public <K> Backend<K> apply(Backend<K> backend) {
        return new DelayingBackend<>(backend);
    }

}
