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

    private final ProxyManagerConfig proxyManagerConfig;

    private final AsyncBackend<K> asyncBackend;
    private final AsyncBackend<K> optimizedAsyncBackend;

    protected AbstractAsyncProxyManager(ProxyManagerConfig proxyManagerConfig) {
        if (proxyManagerConfig.getExpirationAfterWriteStrategy().isPresent() && !isExpireAfterWriteSupported()) {
            throw BucketExceptions.expirationAfterWriteIsNotSupported();
        }
        this.proxyManagerConfig = requireNonNull(proxyManagerConfig);

        this.asyncBackend = new AsyncBackend<K>() {
            @Override
            public <T> CompletableFuture<CommandResult<T>> execute(K key, RemoteCommand<T> command) {
                ExpirationAfterWriteStrategy expirationStrategy = proxyManagerConfig.getExpirationAfterWriteStrategy().orElse(null);
                Request<T> request = new Request<>(command, AbstractAsyncProxyManager.this.proxyManagerConfig.getBackwardCompatibilityVersion(), getClientSideTime(), expirationStrategy);
                Supplier<CompletableFuture<CommandResult<T>>> futureSupplier = () -> AbstractAsyncProxyManager.this.executeAsync(key, request);
                return proxyManagerConfig.getExecutionStrategy().executeAsync(futureSupplier);
            }
        };
        optimizedAsyncBackend = asyncBackend == null ? null : proxyManagerConfig.getSynchronization().apply(asyncBackend, proxyManagerConfig.getSynchronizationListener());
    }

    @Override
    public CompletableFuture<Optional<BucketConfiguration>> getProxyConfiguration(K key) {
        GetConfigurationCommand cmd = new GetConfigurationCommand();
        ExpirationAfterWriteStrategy expirationStrategy = proxyManagerConfig.getExpirationAfterWriteStrategy().orElse(null);
        Request<BucketConfiguration> request = new Request<>(cmd, proxyManagerConfig.getBackwardCompatibilityVersion(), getClientSideTime(), expirationStrategy);
        return executeAsync(key, request).thenApply(result -> {
            if (result.isBucketNotFound()) {
                return Optional.empty();
            }
            return Optional.of(result.getData());
        });
    }

    @Override
    public RemoteAsyncBucketBuilder<K> builder() {
        return getConfig().apply(new DefaultAsyncRemoteBucketBuilder());
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

            return new DefaultAsyncBucketProxy(commandExecutor, configurationSupplier, implicitConfigurationReplacement, listener);
        }

    }

    protected abstract <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request);

    abstract protected CompletableFuture<Void> removeAsync(K key);

    protected ProxyManagerConfig getConfig() {
        return proxyManagerConfig;
    }

    protected long currentTimeNanos() {
        return proxyManagerConfig.getClientSideClock().orElse(TimeMeter.SYSTEM_MILLISECONDS).currentTimeNanos();
    }

    protected Long getClientSideTime() {
        Optional<TimeMeter> clientClock = proxyManagerConfig.getClientSideClock();
        if (clientClock.isEmpty()) {
            return null;
        }
        return clientClock.get().currentTimeNanos();
    }

}
