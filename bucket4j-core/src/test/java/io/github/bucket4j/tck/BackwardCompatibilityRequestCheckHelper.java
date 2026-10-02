package io.github.bucket4j.tck;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface BackwardCompatibilityRequestCheckHelper<K> {

    byte[] execute(K key, byte[] requestBytes) throws ExecutionException, InterruptedException;

    CompletableFuture<byte[]> executeAsync(K key, byte[] requestBytes);

}
