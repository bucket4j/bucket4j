package io.github.bucket4j.tck;

import java.util.concurrent.CompletableFuture;

public interface BackwardCompatibilityRequestCheckHelper<K> {

    byte[] execute(K key, byte[] requestBytes);

    CompletableFuture<byte[]> executeAsync(K key, byte[] requestBytes);

}
