package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;

import java.util.Objects;
import java.util.Optional;

/**
 * TODO
 */
public class ClientSideConfig {

    private static ClientSideConfig defaultConfig = new ClientSideConfig(Versions.getLatest(), Optional.empty());

    private final Version backwardCompatibilityVersion;
    private final Optional<TimeMeter> clientSideClock;

    protected ClientSideConfig(Version backwardCompatibilityVersion, Optional<TimeMeter> clientSideClock) {
        this.backwardCompatibilityVersion = Objects.requireNonNull(backwardCompatibilityVersion);
        this.clientSideClock = Objects.requireNonNull(clientSideClock);
    }

    /**
     * TODO
     */
    public static ClientSideConfig getDefault() {
        return defaultConfig;
    }

    /**
     * TODO
     */
    public static ClientSideConfig backwardCompatibleWith(Version backwardCompatibilityVersion) {
        return new ClientSideConfig(backwardCompatibilityVersion, Optional.empty());
    }

    /**
     * TODO
     */
    public static ClientSideConfig withClientClock(TimeMeter clientClock) {
        return new ClientSideConfig(Versions.getLatest(), Optional.of(clientClock));
    }

    public static ClientSideConfig withClientClockAndCompatibility(TimeMeter clientClock, Version backwardCompatibilityVersion) {
        return new ClientSideConfig(backwardCompatibilityVersion, Optional.of(clientClock));
    }

    /**
     * TODO
     */
    public Optional<TimeMeter> getClientSideClock() {
        return clientSideClock;
    }

    /**
     * TODO
     */
    public Version getBackwardCompatibilityVersion() {
        return backwardCompatibilityVersion;
    }

}
