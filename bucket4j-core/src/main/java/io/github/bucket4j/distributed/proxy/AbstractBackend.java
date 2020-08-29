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
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.BucketProxy;
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

public abstract class AbstractBackend<K> implements Backend<K> {

    private static final RecoveryStrategy DEFAULT_RECOVERY_STRATEGY = RecoveryStrategy.RECONSTRUCT;
    private static final Optimization DEFAULT_REQUEST_OPTIMIZER = Optimization.NONE_OPTIMIZED;

    private final ClientSideConfig clientSideConfig;

    protected AbstractBackend(ClientSideConfig clientSideConfig) {
        this.clientSideConfig = Objects.requireNonNull(clientSideConfig);
    }

    private AsyncBackend<K> asyncView = new AsyncBackend<K>() {
        @Override
        public CompletableFuture<Optional<BucketConfiguration>> getProxyConfiguration(K key) {
            GetConfigurationCommand cmd = new GetConfigurationCommand();
            getClientSideTime();
            Request<BucketConfiguration> request = new Request<>(cmd, getBackwardCompatibilityVersion(), getClientSideTime());
            return executeAsync(key, request).thenApply(result -> {
                if (result.isBucketNotFound()) {
                    return Optional.empty();
                }
                return Optional.of(result.getData());
            });
        }

        @Override
        public RemoteAsyncBucketBuilder<K> builder() {
            return new DefaultAsyncRemoteBucketBuilder();
        }
    };

    @Override
    public AsyncBackend<K> asAsync() {
        if (!isAsyncModeSupported()) {
            throw BucketExceptions.asyncModeIsNotSupported();
        }
        return asyncView;
    }

    @Override
    public RemoteBucketBuilder<K> builder() {
        return new DefaultRemoteBucketBuilder();
    }

    @Override
    public Optional<BucketConfiguration> getProxyConfiguration(K key) {
        GetConfigurationCommand cmd = new GetConfigurationCommand();

        Request<BucketConfiguration> request = new Request<>(cmd, getBackwardCompatibilityVersion(), getClientSideTime());
        CommandResult<BucketConfiguration> result = this.execute(key, request);
        if (result.isBucketNotFound()) {
            return Optional.empty();
        }
        return Optional.of(result.getData());
    }

    private class DefaultAsyncRemoteBucketBuilder implements RemoteAsyncBucketBuilder<K> {

        private RecoveryStrategy recoveryStrategy = DEFAULT_RECOVERY_STRATEGY;
        private Optimization asyncRequestOptimizer = DEFAULT_REQUEST_OPTIMIZER;

        @Override
        public DefaultAsyncRemoteBucketBuilder withRecoveryStrategy(RecoveryStrategy recoveryStrategy) {
            this.recoveryStrategy = Objects.requireNonNull(recoveryStrategy);
            return this;
        }

        @Override
        public DefaultAsyncRemoteBucketBuilder withOptimization(Optimization requestOptimizer) {
            this.asyncRequestOptimizer = Objects.requireNonNull(requestOptimizer);
            return this;
        }

        @Override
        public AsyncBucketProxy buildProxy(K key, BucketConfiguration configuration) {
            if (configuration == null) {
                throw BucketExceptions.nullConfiguration();
            }
            return buildProxy(key, () -> CompletableFuture.completedFuture(configuration));
        }

        @Override
        public AsyncBucketProxy buildProxy(K key, Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier) {
            if (configurationSupplier == null) {
                throw BucketExceptions.nullConfigurationSupplier();
            }

            AsyncCommandExecutor commandExecutor = new AsyncCommandExecutor() {
                @Override
                public <T> CompletableFuture<CommandResult<T>> executeAsync(RemoteCommand<T> command) {
                    Request<T> request = new Request<>(command, getBackwardCompatibilityVersion(), getClientSideTime());
                    return AbstractBackend.this.executeAsync(key, request);
                }
            };
            commandExecutor = asyncRequestOptimizer.apply(commandExecutor);

            return new DefaultAsyncBucketProxy(commandExecutor, recoveryStrategy, configurationSupplier);
        }

    }

    private class DefaultRemoteBucketBuilder implements RemoteBucketBuilder<K> {

        private RecoveryStrategy recoveryStrategy = DEFAULT_RECOVERY_STRATEGY;
        private Optimization requestOptimizer = DEFAULT_REQUEST_OPTIMIZER;

        @Override
        public RemoteBucketBuilder<K> withRecoveryStrategy(RecoveryStrategy recoveryStrategy) {
            this.recoveryStrategy = Objects.requireNonNull(recoveryStrategy);
            return this;
        }

        @Override
        public RemoteBucketBuilder<K> withOptimization(Optimization optimization) {
            this.requestOptimizer = Objects.requireNonNull(optimization);
            return this;
        }

        @Override
        public BucketProxy buildProxy(K key, BucketConfiguration configuration) {
            if (configuration == null) {
                throw BucketExceptions.nullConfiguration();
            }
            return buildProxy(key, () -> configuration);
        }

        @Override
        public BucketProxy buildProxy(K key, Supplier<BucketConfiguration> configurationSupplier) {
            if (configurationSupplier == null) {
                throw BucketExceptions.nullConfigurationSupplier();
            }

            CommandExecutor commandExecutor = new CommandExecutor() {
                @Override
                public <T> CommandResult<T> execute(RemoteCommand<T> command) {
                    Request<T> request = new Request<>(command, getBackwardCompatibilityVersion(), getClientSideTime());
                    return AbstractBackend.this.execute(key, request);
                }
            };
            commandExecutor = requestOptimizer.apply(commandExecutor);

            return new DefaultBucketProxy(configurationSupplier, commandExecutor, recoveryStrategy);
        }

    }

    // TODO javadocs
    abstract protected <T> CommandResult<T> execute(K key, Request<T> request);

    // TODO javadocs
    abstract protected <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request);

    protected ClientSideConfig getClientSideConfig() {
        return clientSideConfig;
    }

    protected Version getBackwardCompatibilityVersion() {
        return clientSideConfig.getBackwardCompatibilityVersion();
    }

    protected Long getClientSideTime() {
        Optional<TimeMeter> clientClock = clientSideConfig.getClientSideClock();
        if (!clientClock.isPresent()) {
            return null;
        }
        return clientClock.get().currentTimeNanos();
    }

}
