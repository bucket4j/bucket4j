
package io.github.bucket4j.grid;

import io.github.bucket4j.BucketConfiguration;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface GridProxy<K extends Serializable> {

    <T extends Serializable> CommandResult<T> execute(K key, GridCommand<T> command);

    void createInitialState(K key, BucketConfiguration configuration);

    <T extends Serializable> T createInitialStateAndExecute(K key, BucketConfiguration configuration, GridCommand<T> command);

    <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, GridCommand<T> command);

    <T extends Serializable> CompletableFuture<T> createInitialStateAndExecuteAsync(K key, BucketConfiguration configuration, GridCommand<T> command);

    Optional<BucketConfiguration> getConfiguration(K key);

    boolean isAsyncModeSupported();

}
