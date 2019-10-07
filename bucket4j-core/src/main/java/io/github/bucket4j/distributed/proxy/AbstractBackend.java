package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.AsyncBucket;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.GetConfigurationCommand;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class AbstractBackend<K extends Serializable> implements Backend<K> {

    @Override
    public Bucket durableProxy(K key, Supplier<BucketConfiguration> configurationSupplier, RequestOptimizer optimizer, RecoveryStrategy recoveryStrategy) {
        CommandExecutor commandExecutor = new CommandExecutor() {
            @Override
            public <T extends Serializable> CommandResult<T> execute(RemoteCommand<T> command) {
                return AbstractBackend.this.execute(key, command);
            }
        };

        return new BucketProxy(configurationSupplier, commandExecutor, recoveryStrategy);
    }

    @Override
    public AsyncBucket asyncDurableProxy(K key, Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier, RequestOptimizer optimizer, RecoveryStrategy recoveryStrategy) {
        if (isAsyncModeSupported()) {
            throw new UnsupportedOperationException();
        }

        AsyncCommandExecutor commandExecutor = new AsyncCommandExecutor() {
            @Override
            public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(RemoteCommand<T> command) {
                return AbstractBackend.this.executeAsync(key, command);
            }
        };

        return new AsyncBucketProxy(commandExecutor, recoveryStrategy, configurationSupplier);
    }

    @Override
    public Optional<BucketConfiguration> getProxyConfiguration(K key) {
        GetConfigurationCommand cmd = new GetConfigurationCommand();
        CommandResult<BucketConfiguration> result = this.execute(key, cmd);
        if (result.isBucketNotFound()) {
            return Optional.empty();
        }
        return Optional.of(result.getData());
    }

    @Override
    public CompletableFuture<Optional<BucketConfiguration>> getProxyConfigurationAsync(K key) {
        if (!isAsyncModeSupported()) {
            throw BucketExceptions.asyncModeIsNotSupported();
        }
        GetConfigurationCommand cmd = new GetConfigurationCommand();
        return this.executeAsync(key, cmd).thenApply(result -> {
            if (result.isBucketNotFound()) {
                return Optional.empty();
            }
            return Optional.of(result.getData());
        });
    }

    // TODO javadocs
    abstract protected <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command);

    // TODO javadocs
    abstract protected <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command);

}
