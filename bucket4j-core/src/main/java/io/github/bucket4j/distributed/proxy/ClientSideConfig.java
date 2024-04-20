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

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.BucketListener;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.AbstractProxyManager.DefaultAsyncRemoteBucketBuilder;
import io.github.bucket4j.distributed.proxy.AbstractProxyManager.DefaultRemoteBucketBuilder;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;

/**
 * Represents additional options for {@link ProxyManager} such as:
 * <ul>
 *     <li>Backward compatibility version, see {@link #backwardCompatibleWith(Version)} for more details.</li>
 *     <li>Client-side clock, see {@link #withClientClock(TimeMeter)} for more details.</li>
 * </ul>
 */
@Deprecated
public class ClientSideConfig {

    private static final ClientSideConfig defaultConfig = new ClientSideConfig(Versions.getLatest(), Optional.empty(),
        ExecutionStrategy.SAME_TREAD, Optional.empty(), Optional.empty(), BucketListener.NOPE, RecoveryStrategy.RECONSTRUCT);

    private final Version backwardCompatibilityVersion;
    private final Optional<TimeMeter> clientSideClock;

    private final ExecutionStrategy executionStrategy;

    private final Optional<Long> requestTimeoutNanos;

    private final Optional<ExpirationAfterWriteStrategy> expirationStrategy;

    private final BucketListener defaultListener;
    private final RecoveryStrategy defaultRecoveryStrategy;

    protected ClientSideConfig(Version backwardCompatibilityVersion, Optional<TimeMeter> clientSideClock,
                               ExecutionStrategy executionStrategy,
                               Optional<Long> requestTimeoutNanos,
                               Optional<ExpirationAfterWriteStrategy> expirationStrategy,
                               BucketListener defaultListener,
                               RecoveryStrategy defaultRecoveryStrategy) {
        this.backwardCompatibilityVersion = Objects.requireNonNull(backwardCompatibilityVersion);
        this.clientSideClock = Objects.requireNonNull(clientSideClock);
        this.executionStrategy = executionStrategy;
        this.requestTimeoutNanos = requestTimeoutNanos;
        this.expirationStrategy = expirationStrategy;
        this.defaultListener = Objects.requireNonNull(defaultListener);
        this.defaultRecoveryStrategy = Objects.requireNonNull(defaultRecoveryStrategy);
    }

    /**
     * Returns default client-side configuration for proxy-manager that configured with following parameters:
     * <ul>
     *     <li><b>Client-clock:</b> is null. This means that server-side clock is always used.</li>
     *     <li><b>Backward compatibility version:</b> is {@code Versions.getLatest()}. This means that compatibility with legacy versions is switched off.</li>
     * </ul>
     *
     * @return default client-side configuration for proxy-manager
     */
    public static ClientSideConfig getDefault() {
        return defaultConfig;
    }

    /**
     * Returns new instance of {@link ClientSideConfig} with configured {@code backwardCompatibilityVersion}.
     *
     * <p>
     * Use this method in case of rolling upgrades, when you want from already new nodes to continue communication using
     * the legacy protocol version which is compatible with {@code backwardCompatibilityVersion}.
     *
     * <p> By default backward compatibility version is {@code Versions.getLatest()}. This means that compatibility with legacy versions is switched off.
     *
     * @param backwardCompatibilityVersion the Bucket4j protocol version to be backward compatible with other nodes in the cluster.
     *
     * @return new instance of {@link ClientSideConfig} with configured {@code backwardCompatibilityVersion}.
     */
    public ClientSideConfig backwardCompatibleWith(Version backwardCompatibilityVersion) {
        return new ClientSideConfig(backwardCompatibilityVersion, clientSideClock, executionStrategy, requestTimeoutNanos, expirationStrategy, defaultListener, defaultRecoveryStrategy);
    }

    /**
     * Returns new instance of {@link ClientSideConfig} with configured {@code clientClock}.
     *
     * <p>
     * Use this method when you want to measure current time by yourself. In normal scenarios you should not use this functionality,
     * but sometimes it can be useful, especially for testing and modeling.
     *
     * <p>
     * By default client-clock is null. This means that server-side clock is always used.
     *
     * @param clientClock the clock that will be used for time measuring instead of server-side clock.
     *
     * @return new instance of {@link ClientSideConfig} with configured {@code clientClock}.
     */
    public ClientSideConfig withClientClock(TimeMeter clientClock) {
        return new ClientSideConfig(backwardCompatibilityVersion, Optional.of(clientClock), executionStrategy, requestTimeoutNanos, expirationStrategy, defaultListener, defaultRecoveryStrategy);
    }

    /**
     * Returns new instance of {@link ClientSideConfig} with configured {@code executionStrategy}.
     *
     * <p>
     * The default executionStrategy is {@link ExecutionStrategy#SAME_TREAD}.
     *
     * @param executionStrategy the strategy for request execution.
     *
     * @return new instance of {@link ClientSideConfig} with configured {@code clientClock}.
     */
    public ClientSideConfig withExecutionStrategy(ExecutionStrategy executionStrategy) {
        return new ClientSideConfig(backwardCompatibilityVersion, clientSideClock, executionStrategy, requestTimeoutNanos, expirationStrategy, defaultListener, defaultRecoveryStrategy);
    }

    /**
     * Returns new instance of {@link ClientSideConfig} with configured timeout for remote operations.
     *
     * <p>
     * The way in which timeout is applied depends on concrete implementation of {@link ProxyManager}. It can be three possible cases:
     * <ol>
     * <li>If underlying technology supports per request timeouts(like timeouts on prepared JDBC statements) then this feature is used by ProxyManager to satisfy requested timeout</li>
     * <li>If timeouts is not supported by underlying technology, but it is possible to deal with timeout on Bucket4j library level(like specifying timeouts on CompletableFuture), then this way is applied.</li>
     * <li>If nothing from above can be applied, then specified {@code requestTimeout} is totally ignored by {@link ProxyManager}</li>
     * </ol>
     *
     * @param requestTimeout timeout for remote operations.
     *
     * @return new instance of {@link ClientSideConfig} with configured {@code requestTimeout}.
     */
    public ClientSideConfig withRequestTimeout(Duration requestTimeout) {
        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw BucketExceptions.nonPositiveRequestTimeout(requestTimeout);
        }
        long requestTimeoutNanos = requestTimeout.toNanos();
        return new ClientSideConfig(backwardCompatibilityVersion, clientSideClock, executionStrategy, Optional.of(requestTimeoutNanos), expirationStrategy, defaultListener, defaultRecoveryStrategy);
    }

    /**
     * Returns new instance of {@link ClientSideConfig} with configured strategy for choosing time to live for buckets.
     * If particular {@link ProxyManager} does not support {@link ExpirationAfterWriteStrategy}
     *  then exception  will be thrown in attempt to construct such ProxyManager with this instance of {@link ClientSideConfig}.
     *
     * @param expirationStrategy the strategy for choosing time to live for buckets.
     *
     * @return new instance of {@link ClientSideConfig} with configured {@code expirationStrategy}.
     */
    public ClientSideConfig withExpirationAfterWriteStrategy(ExpirationAfterWriteStrategy expirationStrategy) {
        return new ClientSideConfig(backwardCompatibilityVersion, clientSideClock, executionStrategy, requestTimeoutNanos, Optional.of(expirationStrategy), defaultListener, defaultRecoveryStrategy);
    }

    /**
     * Returns the strategy for choosing time to live for buckets.
     *
     * @return the strategy for choosing time to live for buckets
     */
    public Optional<ExpirationAfterWriteStrategy> getExpirationAfterWriteStrategy() {
        return expirationStrategy;
    }

    /**
     * Returns clock that will be used for time measurement.
     *
     * @return clock that will be used for time measurement.
     *
     * @see #withClientClock(TimeMeter)
     */
    public Optional<TimeMeter> getClientSideClock() {
        return clientSideClock;
    }

    /**
     * Returns timeout for remote operations
     *
     * @return timeout for remote operations
     */
    public Optional<Long> getRequestTimeoutNanos() {
        return requestTimeoutNanos;
    }

    /**
     * Returns the Bucket4j protocol version is used to be backward compatible with other nodes in the cluster.
     *
     * @return the Bucket4j protocol version is used to be backward compatible with other nodes in the cluster.
     *
     * @see #backwardCompatibleWith(Version)
     */
    public Version getBackwardCompatibilityVersion() {
        return backwardCompatibilityVersion;
    }

    /**
     * Returns the strategy for request execution
     *
     * @return the strategy for request execution
     */
    public ExecutionStrategy getExecutionStrategy() {
        return executionStrategy;
    }

    public <K> RemoteBucketBuilder<K> apply(DefaultRemoteBucketBuilder builder) {
        return builder.withListener(defaultListener)
            .withRecoveryStrategy(defaultRecoveryStrategy);
    }

    public <K> RemoteAsyncBucketBuilder<K> apply(DefaultAsyncRemoteBucketBuilder builder) {
        return builder.withListener(defaultListener)
            .withRecoveryStrategy(defaultRecoveryStrategy);
    }
}
