package io.github.bucket4j.distributed.proxy;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.BucketListener;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;

/**
 * Base class for all proxy-manager builders.
 *
 * @param <K> type of key
 * @param <P> type of proxy manager that is being build
 * @param <B> the type of builder extending AbstractProxyManagerBuilder
 */
public abstract class AbstractProxyManagerBuilder<K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> {

    private Version backwardCompatibilityVersion = Versions.getLatest();
    private Optional<TimeMeter> clientSideClock = Optional.empty();
    private ExecutionStrategy executionStrategy = ExecutionStrategy.SAME_TREAD;
    private Optional<Long> requestTimeoutNanos = Optional.empty();
    private Optional<ExpirationAfterWriteStrategy> expirationStrategy = Optional.empty();

    private BucketListener defaultListener = BucketListener.NOPE;
    private RecoveryStrategy defaultRecoveryStrategy = RecoveryStrategy.RECONSTRUCT;

    /**
     * Configures {@code backwardCompatibilityVersion}.
     *
     * <p>
     * Use this method in case of rolling upgrades, when you want from already new nodes to continue communication using
     * the legacy protocol version which is compatible with {@code backwardCompatibilityVersion}.
     *
     * <p> By default backward compatibility version is {@code Versions.getLatest()}. This means that compatibility with legacy versions is switched off.
     *
     * @param backwardCompatibilityVersion the Bucket4j protocol version to be backward compatible with other nodes in the cluster.
     *
     * @return this builder with configured {@code backwardCompatibilityVersion}.
     */
    public B backwardCompatibleWith(Version backwardCompatibilityVersion) {
        this.backwardCompatibilityVersion = Objects.requireNonNull(backwardCompatibilityVersion);
        return (B) this;
    }

    /**
     * Configures {@code clientClock}.
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
     * @return this builder with configured {@code clientClock}.
     */
    public B clientClock(TimeMeter clientClock) {
        Objects.requireNonNull(clientSideClock);
        if (!clientClock.isWallClockBased()) {
            throw BucketExceptions.isNotWallBasedClockUsedInDistributedEnvironment(clientClock.getClass());
        }
        this.clientSideClock = Optional.of(clientClock);
        return (B) this;
    }

    /**
     * Configures {@code executionStrategy}.
     *
     * <p>
     * The default executionStrategy is {@link ExecutionStrategy#SAME_TREAD}.
     *
     * @param executionStrategy the strategy for request execution.
     *
     * @return this builder with configured {@code clientClock}.
     */
    public B executionStrategy(ExecutionStrategy executionStrategy) {
        this.executionStrategy = Objects.requireNonNull(executionStrategy);
        return (B) this;
    }

    /**
     * Configures timeout for remote operations.
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
     * @return this builder with configured {@code requestTimeout}.
     */
    public B requestTimeout(Duration requestTimeout) {
        Objects.requireNonNull(requestTimeout);
        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw BucketExceptions.nonPositiveRequestTimeout(requestTimeout);
        }
        this.requestTimeoutNanos = Optional.of(requestTimeout.toNanos());
        return (B) this;
    }

    /**
     * Configures strategy for choosing time to live for buckets.
     *
     * If particular {@link P} does not support {@link ExpirationAfterWriteStrategy} then this method throws exception.
     *
     * @param expirationStrategy the strategy for choosing time to live for buckets.
     *
     * @return this builder with configured {@code expirationStrategy}.
     */
    public B expirationAfterWrite(ExpirationAfterWriteStrategy expirationStrategy) {
        if (expirationStrategy != null && !isExpireAfterWriteSupported()) {
            throw BucketExceptions.expirationAfterWriteIsNotSupported();
        }
        this.expirationStrategy = Optional.ofNullable(expirationStrategy);
        return (B) this;
    }

    /**
     * Configures listener at proxy-manager level, this listener will be used for buckets in case of listener will not be specified during bucket build time.
     *
     * @param defaultListener listener of bucket events
     *
     * @return this builder instance
     */
    public B defaultListener(BucketListener defaultListener) {
        this.defaultListener = Objects.requireNonNull(defaultListener);
        return (B) this;
    }

    /**
     * Configures custom recovery strategy  at proxy-manager level, this strategy will be used for buckets in case of it will not be specified during bucket build time.
     *
     * @param recoveryStrategy specifies the reaction which should be applied in case of previously saved state of bucket has been lost, explicitly removed or expired.
     *
     * @return {@code this}
     */
    public B defaultRecoveryStrategy(RecoveryStrategy recoveryStrategy) {
        this.defaultRecoveryStrategy = Objects.requireNonNull(recoveryStrategy);
        return (B) this;
    }

    /**
     * Returns the strategy for choosing time to live for buckets.
     *
     * @return the strategy for choosing time to live for buckets
     */
    public Optional<ExpirationAfterWriteStrategy> getExpirationAfterWrite() {
        return expirationStrategy;
    }

    /**
     * Returns clock that will be used for time measurement.
     *
     * @return clock that will be used for time measurement.
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

    /**
     * Builds new instance of {@link P}
     *
     * @return new instance of {@link P}
     */
    abstract public P build();

    /**
     * Describes whether {@link P} supports expire-after-write feature.
     *
     * @return <code>true</code> {@link P} supports expire-after-write feature.
     */
    public boolean isExpireAfterWriteSupported() {
        return false;
    }

    public ClientSideConfig getClientSideConfig() {
        return new ClientSideConfig(backwardCompatibilityVersion, clientSideClock, executionStrategy, requestTimeoutNanos, expirationStrategy, defaultListener, defaultRecoveryStrategy);
    }

}
