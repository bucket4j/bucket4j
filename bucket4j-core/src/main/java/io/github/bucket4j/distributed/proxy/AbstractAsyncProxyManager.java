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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.BucketListener;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronization;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.remote.commands.GetConfigurationCommand;

import static java.util.Objects.requireNonNull;

public abstract class AbstractAsyncProxyManager<K> implements AsyncProxyManager<K> {

    private static final BucketSynchronization DEFAULT_REQUEST_OPTIMIZER = BucketSynchronization.NONE_OPTIMIZED;

    private final AsyncProxyManagerConfig<K> config;

    private final AsyncBackend<K> asyncBackend;
    private final AsyncBackend<K> optimizedAsyncBackend;

    protected AbstractAsyncProxyManager(AsyncProxyManagerConfig<K> config) {
        if (config.getExpirationAfterWriteStrategy().isPresent() && !isExpireAfterWriteSupported()) {
            throw BucketExceptions.expirationAfterWriteIsNotSupported();
        }
        this.config = requireNonNull(config);

        this.asyncBackend = new AsyncBackend<>() {
            @Override
            public <T> CompletableFuture<CommandResult<T>> execute(K key, RemoteCommand<T> command) {
                ExpirationAfterWriteStrategy expirationStrategy = config.getExpirationAfterWriteStrategy().orElse(null);
                Request<T> request = new Request<>(command, config.getBackwardCompatibilityVersion(), getClientSideTime(), expirationStrategy);
                Supplier<CompletableFuture<CommandResult<T>>> futureSupplier = () -> AbstractAsyncProxyManager.this.executeAsync(key, request);
                return config.getExecutionStrategy().execute(futureSupplier);
            }
        };
        optimizedAsyncBackend = asyncBackend == null ? null : config.getSynchronization().apply(asyncBackend);
    }

    @Override
    public CompletableFuture<Optional<BucketConfiguration>> getProxyConfiguration(K key) {
        GetConfigurationCommand cmd = new GetConfigurationCommand();
        ExpirationAfterWriteStrategy expirationStrategy = config.getExpirationAfterWriteStrategy().orElse(null);
        Request<BucketConfiguration> request = new Request<>(cmd, config.getBackwardCompatibilityVersion(), getClientSideTime(), expirationStrategy);
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

    @Override
    public CompletableFuture<Void> removeProxy(K key) {
        return removeAsync(key);
    }

    class DefaultAsyncRemoteBucketBuilder implements RemoteAsyncBucketBuilder<K> {

        private BucketSynchronization asyncRequestOptimizer = DEFAULT_REQUEST_OPTIMIZER;
        private ImplicitConfigurationReplacement implicitConfigurationReplacement;
        private BucketListener listener = BucketListener.NOPE;

        @Override
        public DefaultAsyncRemoteBucketBuilder withSynchronization(BucketSynchronization requestOptimizer) {
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

            return new DefaultAsyncBucketProxy(commandExecutor, configurationSupplier, implicitConfigurationReplacement,
                listener == BucketListener.NOPE ? config.getListenerProvider().get(key) : listener);
        }

    }

    protected abstract <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request);

    abstract protected CompletableFuture<Void> removeAsync(K key);

    protected AsyncProxyManagerConfig<K> getConfig() {
        return config;
    }

    protected long currentTimeNanos() {
        return config.getClientSideClock().orElse(TimeMeter.SYSTEM_MILLISECONDS).currentTimeNanos();
    }

    protected Long getClientSideTime() {
        Optional<TimeMeter> clientClock = config.getClientSideClock();
        if (clientClock.isEmpty()) {
            return null;
        }
        return clientClock.get().currentTimeNanos();
    }

}
