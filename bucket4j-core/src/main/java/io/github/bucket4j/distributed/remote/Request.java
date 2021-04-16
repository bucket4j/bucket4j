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

import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;

import java.io.IOException;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class Request<T> {

    private final Version backwardCompatibilityVersion;
    private final RemoteCommand<T> command;
    private final Long clientSideTime;

    public Request(RemoteCommand<T> command, Version backwardCompatibilityVersion, Long clientSideTime) {
        this.command = command;
        this.backwardCompatibilityVersion = backwardCompatibilityVersion;
        this.clientSideTime = clientSideTime;
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

    // TODO add testcase for serialization
    public static SerializationHandle<Request> SERIALIZATION_HANDLE = new SerializationHandle<Request>() {
        @Override
        public <S> Request deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            int backwardCompatibilityNumber = adapter.readInt(input);
            Versions.check(backwardCompatibilityNumber, v_7_0_0, v_7_0_0);
            backwardCompatibilityVersion = Versions.byNumber(backwardCompatibilityNumber);

            RemoteCommand<?> command = RemoteCommand.deserialize(adapter, input, backwardCompatibilityVersion);
            backwardCompatibilityVersion = Versions.byNumber(backwardCompatibilityNumber);

            Long clientTime = null;
            boolean clientTimeProvided = adapter.readBoolean(input);
            if (clientTimeProvided) {
                clientTime = adapter.readLong(input);
            }

            return new Request(command, backwardCompatibilityVersion, clientTime);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, Request request, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());
            adapter.writeInt(output, backwardCompatibilityVersion.getNumber());

            RemoteCommand.serialize(adapter, output, request.command, backwardCompatibilityVersion);

            if (request.clientSideTime != null) {
                adapter.writeBoolean(output, true);
                adapter.writeLong(output, request.clientSideTime);
            } else {
                adapter.writeBoolean(output, false);
            }
        }

        @Override
        public int getTypeId() {
            return 37;
        }

        @Override
        public Class<Request> getSerializedType() {
            return Request.class;
        }

    };

}
