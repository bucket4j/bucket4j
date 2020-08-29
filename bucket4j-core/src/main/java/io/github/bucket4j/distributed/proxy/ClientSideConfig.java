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
