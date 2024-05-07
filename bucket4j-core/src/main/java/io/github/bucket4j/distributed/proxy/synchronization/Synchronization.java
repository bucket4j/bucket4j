package io.github.bucket4j.distributed.proxy.synchronization;

import io.github.bucket4j.distributed.proxy.AsyncBackend;
import io.github.bucket4j.distributed.proxy.Backend;

public interface Synchronization {

     <K> Backend<K> apply(Backend<K> backend);

     <K> AsyncBackend<K> apply(AsyncBackend<K> backend);

}
