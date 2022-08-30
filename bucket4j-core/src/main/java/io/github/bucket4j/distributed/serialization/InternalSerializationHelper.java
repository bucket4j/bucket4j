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
package io.github.bucket4j.distributed.serialization;

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.versioning.Version;

import java.io.*;

public class InternalSerializationHelper {

    public static byte[] serializeState(RemoteBucketState state, Version backwardCompatibilityVersion) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(byteStream);

            RemoteBucketState.SERIALIZATION_HANDLE.serialize(DataOutputSerializationAdapter.INSTANCE, output, state, backwardCompatibilityVersion, Scope.PERSISTED_STATE);

            output.close();
            byteStream.close();

            return byteStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static RemoteBucketState deserializeState(byte[] bytes) {
        try {
            try (DataInputStream inputSteam = new DataInputStream(new ByteArrayInputStream(bytes))) {
                return RemoteBucketState.SERIALIZATION_HANDLE.deserialize(DataOutputSerializationAdapter.INSTANCE, inputSteam);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] serializeRequest(Request<?> request) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(byteStream);

            Request.SERIALIZATION_HANDLE.serialize(DataOutputSerializationAdapter.INSTANCE, output, request, request.getBackwardCompatibilityVersion(), Scope.REQUEST);
            
            output.close();
            byteStream.close();

            return byteStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> Request<T> deserializeRequest(byte[] bytes) {
        try {
            try (DataInputStream inputSteam = new DataInputStream(new ByteArrayInputStream(bytes))) {
                return (Request<T>) Request.SERIALIZATION_HANDLE.deserialize(DataOutputSerializationAdapter.INSTANCE, inputSteam);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] serializeResult(CommandResult<?> result, Version backwardCompatibilityVersion) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(byteStream);

            CommandResult.SERIALIZATION_HANDLE.serialize(DataOutputSerializationAdapter.INSTANCE, output, result, backwardCompatibilityVersion, Scope.RESPONSE);

            output.close();
            byteStream.close();

            return byteStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> CommandResult<T> deserializeResult(byte[] bytes, Version backwardCompatibilityVersion) {
        try {
            try (DataInputStream inputSteam = new DataInputStream(new ByteArrayInputStream(bytes))) {
                return (CommandResult<T>) CommandResult.SERIALIZATION_HANDLE.deserialize(DataOutputSerializationAdapter.INSTANCE, inputSteam);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
