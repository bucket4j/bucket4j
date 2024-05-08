/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.BucketListener;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.optimization.Optimization;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.remote.commands.GetConfigurationCommand;
import io.github.bucket4j.distributed.versioning.Version;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public abstract class AbstractProxyManager<K> implements ProxyManager<K> {

    private static final RecoveryStrategy DEFAULT_RECOVERY_STRATEGY = RecoveryStrategy.RECONSTRUCT;
    private static final Optimization DEFAULT_REQUEST_OPTIMIZER = Optimization.NONE_OPTIMIZED;

    private final ClientSideConfig clientSideConfig;

    private final Backend<K> backend;
    private final Backend<K> optimizedBackend;
    private final AsyncBackend<K> asyncBackend;
    private final AsyncBackend<K> optimizedAsyncBackend;

    protected AbstractProxyManager(ClientSideConfig clientSideConfig) {
        if (clientSideConfig.getExpirationAfterWriteStrategy().isPresent() && !isExpireAfterWriteSupported()) {
            throw BucketExceptions.expirationAfterWriteIsNotSupported();
        }
        this.clientSideConfig = requireNonNull(clientSideConfig);
        this.backend = new Backend<K>() {
            @Override
            public <T> CommandResult<T> execute(K key, RemoteCommand<T> command) {
                ExpirationAfterWriteStrategy expirationStrategy = clientSideConfig.getExpirationAfterWriteStrategy().orElse(null);
                Request<T> request = new Request<>(command, getBackwardCompatibilityVersion(), getClientSideTime(), expirationStrategy);
                Supplier<CommandResult<T>> resultSupplier = () -> AbstractProxyManager.this.execute(key, request);
                return clientSideConfig.getExecutionStrategy().execute(resultSupplier);
            }
        };
        optimizedBackend = clientSideConfig.getSynchronization().apply(backend);

        this.asyncBackend = !isAsyncModeSupported() ? null : new AsyncBackend<K>() {
            @Override
            public <T> CompletableFuture<CommandResult<T>> execute(K key, RemoteCommand<T> command) {
                ExpirationAfterWriteStrategy expirationStrategy = clientSideConfig.getExpirationAfterWriteStrategy().orElse(null);
                Request<T> request = new Request<>(command, getBackwardCompatibilityVersion(), getClientSideTime(), expirationStrategy);
                Supplier<CompletableFuture<CommandResult<T>>> futureSupplier = () -> AbstractProxyManager.this.executeAsync(key, request);
                return clientSideConfig.getExecutionStrategy().executeAsync(futureSupplier);
            }
        };
        optimizedAsyncBackend = asyncBackend == null ? null : clientSideConfig.getSynchronization().apply(asyncBackend);
    }

    private final AsyncProxyManager<K> asyncView = new AsyncProxyManager<>() {
        @Override
        public CompletableFuture<Optional<BucketConfiguration>> getProxyConfiguration(K key) {
            GetConfigurationCommand cmd = new GetConfigurationCommand();
            ExpirationAfterWriteStrategy expirationStrategy = clientSideConfig.getExpirationAfterWriteStrategy().orElse(null);
            Request<BucketConfiguration> request = new Request<>(cmd, getBackwardCompatibilityVersion(), getClientSideTime(), expirationStrategy);
            return executeAsync(key, request).thenApply(result -> {
                if (result.isBucketNotFound()) {
                    return Optional.empty();
                }
                return Optional.of(result.getData());
            });
        }

        @Override
        public RemoteAsyncBucketBuilder<K> builder() {
            return getClientSideConfig().apply(new DefaultAsyncRemoteBucketBuilder());
        }

        @Override
        public CompletableFuture<Void> removeProxy(K key) {
            return removeAsync(key);
        }
    };

    @Override
    public AsyncProxyManager<K> asAsync() {
        if (!isAsyncModeSupported()) {
            throw BucketExceptions.asyncModeIsNotSupported();
        }
        return asyncView;
    }

    @Override
    public RemoteBucketBuilder<K> builder() {
        return getClientSideConfig().apply(new DefaultRemoteBucketBuilder());
    }

    @Override
    public Optional<BucketConfiguration> getProxyConfiguration(K key) {
        GetConfigurationCommand cmd = new GetConfigurationCommand();

        Request<BucketConfiguration> request = new Request<>(cmd, getBackwardCompatibilityVersion(), getClientSideTime(), null);
        CommandResult<BucketConfiguration> result = this.execute(key, request);
        if (result.isBucketNotFound()) {
            return Optional.empty();
        }
        return Optional.of(result.getData());
    }

    class DefaultAsyncRemoteBucketBuilder implements RemoteAsyncBucketBuilder<K> {

        private RecoveryStrategy recoveryStrategy = DEFAULT_RECOVERY_STRATEGY;
        private Optimization asyncRequestOptimizer = DEFAULT_REQUEST_OPTIMIZER;
        private ImplicitConfigurationReplacement implicitConfigurationReplacement;
        private BucketListener listener = BucketListener.NOPE;

        @Override
        public DefaultAsyncRemoteBucketBuilder withRecoveryStrategy(RecoveryStrategy recoveryStrategy) {
            this.recoveryStrategy = requireNonNull(recoveryStrategy);
            return this;
        }

        @Override
        public DefaultAsyncRemoteBucketBuilder withOptimization(Optimization requestOptimizer) {
            this.asyncRequestOptimizer = requireNonNull(requestOptimizer);
            return this;
        }

        @Override
        public DefaultAsyncRemoteBucketBuilder withImplicitConfigurationReplacement(long desiredConfigurationVersion, TokensInheritanceStrategy tokensInheritanceStrategy) {
            this.implicitConfigurationReplacement = new ImplicitConfigurationReplacement(desiredConfigurationVersion, requireNonNull(tokensInheritanceStrategy));
            return this;
        }

        @Override
        public RemoteAsyncBucketBuilder<K> withListener(BucketListener listener) {
            this.listener = Objects.requireNonNull(listener);
            return this;
        }

        @Override
        public AsyncBucketProxy build(K key, BucketConfiguration configuration) {
            if (configuration == null) {
                throw BucketExceptions.nullConfiguration();
            }
            return build(key, () -> CompletableFuture.completedFuture(configuration));
        }

        @Override
        public AsyncBucketProxy build(K key, Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier) {
            if (configurationSupplier == null) {
                throw BucketExceptions.nullConfigurationSupplier();
            }

            AsyncBackend<K> backend = asyncRequestOptimizer == DEFAULT_REQUEST_OPTIMIZER? optimizedAsyncBackend : asyncBackend;

            AsyncCommandExecutor commandExecutor = new AsyncCommandExecutor() {
                @Override
                public <T> CompletableFuture<CommandResult<T>> executeAsync(RemoteCommand<T> command) {
                    return backend.execute(key, command);
                }
            };
            commandExecutor = asyncRequestOptimizer.apply(commandExecutor);

            return new DefaultAsyncBucketProxy(commandExecutor, recoveryStrategy, configurationSupplier, implicitConfigurationReplacement, listener);
        }

    }

    class DefaultRemoteBucketBuilder implements RemoteBucketBuilder<K> {

        private RecoveryStrategy recoveryStrategy = DEFAULT_RECOVERY_STRATEGY;
        private Optimization requestOptimizer = DEFAULT_REQUEST_OPTIMIZER;
        private ImplicitConfigurationReplacement implicitConfigurationReplacement;
        private BucketListener listener = BucketListener.NOPE;

        @Override
        public RemoteBucketBuilder<K> withRecoveryStrategy(RecoveryStrategy recoveryStrategy) {
            this.recoveryStrategy = requireNonNull(recoveryStrategy);
            return this;
        }

        @Override
        public RemoteBucketBuilder<K> withOptimization(Optimization optimization) {
            this.requestOptimizer = requireNonNull(optimization);
            return this;
        }

        @Override
        public RemoteBucketBuilder<K> withImplicitConfigurationReplacement(long desiredConfigurationVersion, TokensInheritanceStrategy tokensInheritanceStrategy) {
            this.implicitConfigurationReplacement = new ImplicitConfigurationReplacement(desiredConfigurationVersion, requireNonNull(tokensInheritanceStrategy));
            return this;
        }

        @Override
        public RemoteBucketBuilder<K> withListener(BucketListener listener) {
            this.listener = Objects.requireNonNull(listener);
            return this;
        }

        @Override
        public BucketProxy build(K key, BucketConfiguration configuration) {
            if (configuration == null) {
                throw BucketExceptions.nullConfiguration();
            }
            return build(key, () -> configuration);
        }

        @Override
        public BucketProxy build(K key, Supplier<BucketConfiguration> configurationSupplier) {
            if (configurationSupplier == null) {
                throw BucketExceptions.nullConfigurationSupplier();
            }

            Backend<K> backend = requestOptimizer == DEFAULT_REQUEST_OPTIMIZER ? optimizedBackend : AbstractProxyManager.this.backend;

            CommandExecutor commandExecutor = new CommandExecutor() {
                @Override
                public <T> CommandResult<T> execute(RemoteCommand<T> command) {
                    return backend.execute(key, command);
                }
            };
            commandExecutor = requestOptimizer.apply(commandExecutor);

            return new DefaultBucketProxy(configurationSupplier, commandExecutor, recoveryStrategy, implicitConfigurationReplacement, listener);
        }

    }

    protected abstract <T> CommandResult<T> execute(K key, Request<T> request);

    protected abstract <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request);

    abstract protected CompletableFuture<Void> removeAsync(K key);

    protected ClientSideConfig getClientSideConfig() {
        return clientSideConfig;
    }

    protected Version getBackwardCompatibilityVersion() {
        return clientSideConfig.getBackwardCompatibilityVersion();
    }

    protected long currentTimeNanos() {
        return clientSideConfig.getClientSideClock().orElse(TimeMeter.SYSTEM_MILLISECONDS).currentTimeNanos();
    }

    protected Long getClientSideTime() {
        Optional<TimeMeter> clientClock = clientSideConfig.getClientSideClock();
        if (clientClock.isEmpty()) {
            return null;
        }
        return clientClock.get().currentTimeNanos();
    }

}
