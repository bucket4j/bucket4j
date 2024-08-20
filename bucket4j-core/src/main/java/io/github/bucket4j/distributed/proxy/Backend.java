package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;

public interface Backend<K> {

    <T> CommandResult<T> execute(K key, RemoteCommand<T> command);

}
