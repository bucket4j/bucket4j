package io.github.bucket4j.distributed.proxy;

import java.util.concurrent.CompletableFuture;

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;

public interface AsyncBackend<K> {

    <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request);

}
