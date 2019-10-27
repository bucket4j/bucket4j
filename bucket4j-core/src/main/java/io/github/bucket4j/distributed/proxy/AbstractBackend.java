package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.AsyncBucket;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.GetConfigurationCommand;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class AbstractBackend<K extends Serializable> implements Backend<K> {

    private static final RecoveryStrategy DEFAULT_RECOVERY_STRATEGY = RecoveryStrategy.RECONSTRUCT;
    private static final RequestOptimizer DEFAULT_REQUEST_OPTIMIZER = RequestOptimizer.NONE_OPTIMIZED;
    private static final AsyncRequestOptimizer DEFAULT_ASYNC_REQUEST_OPTIMIZER = AsyncRequestOptimizer.NONE_OPTIMIZED;

    @Override
    public RemoteBucketBuilder<K> builder() {
        return new DefaultRemoteBucketBuilder();
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

    private class DefaultRemoteBucketBuilder implements RemoteBucketBuilder<K> {

        private RecoveryStrategy recoveryStrategy = DEFAULT_RECOVERY_STRATEGY;
        private RequestOptimizer requestOptimizer = DEFAULT_REQUEST_OPTIMIZER;
        private AsyncRequestOptimizer asyncRequestOptimizer = DEFAULT_ASYNC_REQUEST_OPTIMIZER;

        @Override
        public RemoteBucketBuilder<K> withRecoveryStrategy(RecoveryStrategy recoveryStrategy) {
            this.recoveryStrategy = Objects.requireNonNull(recoveryStrategy);
            return this;
        }

        @Override
        public RemoteBucketBuilder<K> withRequestOptimizer(RequestOptimizer requestOptimizer) {
            this.requestOptimizer = Objects.requireNonNull(requestOptimizer);
            return this;
        }

        @Override
        public RemoteBucketBuilder<K> withAsyncRequestOptimizer(AsyncRequestOptimizer requestOptimizer) {
            this.asyncRequestOptimizer = Objects.requireNonNull(requestOptimizer);
            return this;
        }

        @Override
        public Bucket buildProxy(K key, BucketConfiguration configuration) {
            if (configuration == null) {
                throw BucketExceptions.nullConfiguration();
            }
            return buildProxy(key, () -> configuration);
        }

        @Override
        public Bucket buildProxy(K key, Supplier<BucketConfiguration> configurationSupplier) {
            if (configurationSupplier == null) {
                throw BucketExceptions.nullConfigurationSupplier();
            }

            CommandExecutor commandExecutor = new CommandExecutor() {
                @Override
                public <T extends Serializable> CommandResult<T> execute(RemoteCommand<T> command) {
                    return AbstractBackend.this.execute(key, command);
                }
            };
            commandExecutor = requestOptimizer.optimize(commandExecutor);

            return new BucketProxy(configurationSupplier, commandExecutor, recoveryStrategy);
        }

        @Override
        public AsyncBucket buildAsyncProxy(K key, BucketConfiguration configuration) {
            if (configuration == null) {
                throw BucketExceptions.nullConfiguration();
            }
            return buildAsyncProxy(key, () -> CompletableFuture.completedFuture(configuration));
        }

        @Override
        public AsyncBucket buildAsyncProxy(K key, Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier) {
            if (!isAsyncModeSupported()) {
                throw new UnsupportedOperationException();
            }

            if (configurationSupplier == null) {
                throw BucketExceptions.nullConfigurationSupplier();
            }

            AsyncCommandExecutor commandExecutor = new AsyncCommandExecutor() {
                @Override
                public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(RemoteCommand<T> command) {
                    return AbstractBackend.this.executeAsync(key, command);
                }
            };
            commandExecutor = asyncRequestOptimizer.optimize(commandExecutor);

            return new AsyncBucketProxy(commandExecutor, recoveryStrategy, configurationSupplier);
        }

    }

    // TODO javadocs
    abstract protected <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command);

    // TODO javadocs
    abstract protected <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command);

}
