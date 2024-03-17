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
package io.github.bucket4j.distributed.remote;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;
import static io.github.bucket4j.distributed.versioning.Versions.v_8_10_0;

public class Request<T> implements ComparableByContent<Request<T>> {

    private final Version backwardCompatibilityVersion;
    private final RemoteCommand<T> command;
    private final Long clientSideTime;
    private final ExpirationAfterWriteStrategy expirationStrategy;

    public Request(RemoteCommand<T> command, Version backwardCompatibilityVersion, Long clientSideTime, ExpirationAfterWriteStrategy expirationStrategy) {
        this.command = command;
        this.clientSideTime = clientSideTime;
        this.backwardCompatibilityVersion = backwardCompatibilityVersion;
        this.expirationStrategy = expirationStrategy;
    }

    public RemoteCommand<T> getCommand() {
        return command;
    }

    public Version getBackwardCompatibilityVersion() {
        return backwardCompatibilityVersion;
    }

    public Long getClientSideTime() {
        return clientSideTime;
    }

    public ExpirationAfterWriteStrategy getExpirationStrategy() {
        return expirationStrategy;
    }

    public static SerializationHandle<Request<?>> SERIALIZATION_HANDLE = new SerializationHandle<>() {
        @Override
        public <S> Request<?> deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_8_10_0);

            int backwardCompatibilityNumber = adapter.readInt(input);
            Version requestBackwardCompatibilityVersion = Versions.byNumber(backwardCompatibilityNumber);

            RemoteCommand<?> command = RemoteCommand.deserialize(adapter, input);

            Long clientTime = null;
            boolean clientTimeProvided = adapter.readBoolean(input);
            if (clientTimeProvided) {
                clientTime = adapter.readLong(input);
            }

            ExpirationAfterWriteStrategy expireStrategy = null;
            if (formatNumber >= v_8_10_0.getNumber()) {
                boolean hasExpireStrategy = adapter.readBoolean(input);
                if (hasExpireStrategy) {
                    expireStrategy = ExpirationAfterWriteStrategy.deserialize(adapter, input);
                }
            }

            return new Request<>(command, requestBackwardCompatibilityVersion, clientTime, expireStrategy);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, Request<?> request, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Version selfVersion = request.getSelfVersion();
            Version effectiveVersion = Versions.max(request.command.getRequiredVersion(), selfVersion);
            Versions.check(effectiveVersion.getNumber(), v_7_0_0, request.backwardCompatibilityVersion);

            adapter.writeInt(output, selfVersion.getNumber());
            adapter.writeInt(output, effectiveVersion.getNumber());

            RemoteCommand.serialize(adapter, output, request.command, backwardCompatibilityVersion, scope);

            if (request.clientSideTime != null) {
                adapter.writeBoolean(output, true);
                adapter.writeLong(output, request.clientSideTime);
            } else {
                adapter.writeBoolean(output, false);
            }
            if (selfVersion.getNumber() >= v_8_10_0.getNumber()) {
                if (request.expirationStrategy != null) {
                    adapter.writeBoolean(output, true);
                    ExpirationAfterWriteStrategy.serialize(adapter, output, request.expirationStrategy, backwardCompatibilityVersion, scope);
                } else {
                    adapter.writeBoolean(output, false);
                }
            }
        }

        @Override
        public int getTypeId() {
            return 37;
        }

        @Override
        public Class<Request<?>> getSerializedType() {
            return (Class) Request.class;
        }

        @Override
        public Request<?> fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_8_10_0);

            int backwardCompatibilityNumber = readIntValue(snapshot, "backwardCompatibilityNumber");
            Version requestBackwardCompatibilityVersion = Versions.byNumber(backwardCompatibilityNumber);

            RemoteCommand<?> command = RemoteCommand.fromJsonCompatibleSnapshot((Map<String, Object>) snapshot.get("command"));

            Long clientTime = null;
            if (snapshot.containsKey("clientTime")) {
                clientTime = readLongValue(snapshot, "clientTime");
            }

            ExpirationAfterWriteStrategy expireStrategy = null;
            if (snapshot.containsKey("expireAfterWriteStrategy")) {
                expireStrategy = ExpirationAfterWriteStrategy.fromJsonCompatibleSnapshot((Map<String, Object>) snapshot.get("expireAfterWriteStrategy"));
            }

            return new Request<>(command, requestBackwardCompatibilityVersion, clientTime, expireStrategy);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(Request<?> request, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Version effectiveVersion = Versions.max(request.command.getRequiredVersion(), request.getSelfVersion());;
            Versions.check(effectiveVersion.getNumber(), v_7_0_0, request.backwardCompatibilityVersion);

            Version selfVersion = request.getSelfVersion();
            Map<String, Object> result = new HashMap<>();
            result.put("version", selfVersion.getNumber());
            result.put("backwardCompatibilityNumber", effectiveVersion.getNumber());
            result.put("command", RemoteCommand.toJsonCompatibleSnapshot(request.command, backwardCompatibilityVersion, scope));
            if (request.clientSideTime != null) {
                result.put("clientTime", request.clientSideTime);
            }
            if (request.expirationStrategy != null) {
                result.put("expireAfterWriteStrategy", ExpirationAfterWriteStrategy.toJsonCompatibleSnapshot(request.expirationStrategy, backwardCompatibilityVersion, scope));
            }
            return result;
        }

        @Override
        public String getTypeName() {
            return "Request";
        }

    };

    private Version getSelfVersion() {
        return expirationStrategy != null ? v_8_10_0 : v_7_0_0;
    }

    @Override
    public boolean equalsByContent(Request<T> other) {
        return // backwardCompatibilityVersion.equals(other.backwardCompatibilityVersion) &&
                ComparableByContent.equals(command, other.command)
            && Objects.equals(clientSideTime, other.clientSideTime);
    }

}