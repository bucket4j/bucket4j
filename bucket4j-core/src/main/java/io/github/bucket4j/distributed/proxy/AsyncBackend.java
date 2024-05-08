package io.github.bucket4j.distributed.proxy;

import java.util.concurrent.CompletableFuture;

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.Request;

public interface AsyncBackend<K> {

    <T> CompletableFuture<CommandResult<T>> execute(K key, RemoteCommand<T> remoteCommand);

}
