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
import java.nio.ByteBuffer;

public class InternalSerializationHelper {

    public static byte[] serializeState(RemoteBucketState state, Version backwardCompatibilityVersion) {
        return serializeState(state, backwardCompatibilityVersion, SerializationStyle.DATA_OUTPUT);
    }

    public static byte[] serializeState(RemoteBucketState state, Version backwardCompatibilityVersion, SerializationStyle style) {
        return serialize(RemoteBucketState.SERIALIZATION_HANDLE, state, backwardCompatibilityVersion, Scope.PERSISTED_STATE, style);
    }

    public static RemoteBucketState deserializeState(byte[] bytes) {
        return deserializeState(bytes, SerializationStyle.DATA_OUTPUT);
    }

    public static RemoteBucketState deserializeState(byte[] bytes, SerializationStyle style) {
        return deserialize(RemoteBucketState.SERIALIZATION_HANDLE, bytes, style);
    }

    public static byte[] serializeRequest(Request<?> request) {
        return serializeRequest(request, SerializationStyle.DATA_OUTPUT);
    }

    public static byte[] serializeRequest(Request<?> request, SerializationStyle style) {
        return serialize(Request.SERIALIZATION_HANDLE, request, request.getBackwardCompatibilityVersion(), Scope.REQUEST, style);
    }

    public static <T> Request<T> deserializeRequest(byte[] bytes) {
        return deserializeRequest(bytes, SerializationStyle.DATA_OUTPUT);
    }

    @SuppressWarnings("unchecked")
    public static <T> Request<T> deserializeRequest(byte[] bytes, SerializationStyle style) {
        return (Request<T>) deserialize(Request.SERIALIZATION_HANDLE, bytes, style);
    }

    public static byte[] serializeResult(CommandResult<?> result, Version backwardCompatibilityVersion) {
        return serializeResult(result, backwardCompatibilityVersion, SerializationStyle.DATA_OUTPUT);
    }

    public static byte[] serializeResult(CommandResult<?> result, Version backwardCompatibilityVersion, SerializationStyle style) {
        return serialize(CommandResult.SERIALIZATION_HANDLE, result, backwardCompatibilityVersion, Scope.RESPONSE, style);
    }

    public static <T> CommandResult<T> deserializeResult(byte[] bytes, Version backwardCompatibilityVersion) {
        return deserializeResult(bytes, backwardCompatibilityVersion, SerializationStyle.DATA_OUTPUT);
    }

    @SuppressWarnings("unchecked")
    public static <T> CommandResult<T> deserializeResult(byte[] bytes, Version backwardCompatibilityVersion, SerializationStyle style) {
        return (CommandResult<T>) deserialize(CommandResult.SERIALIZATION_HANDLE, bytes, style);
    }

    private static <T> byte[] serialize(SerializationHandle<T> handle, T serializableObject, Version backwardCompatibilityVersion, Scope scope, SerializationStyle style) {
        try {
            return switch (style) {
                case DATA_OUTPUT -> serializeViaDataOutput(handle, serializableObject, backwardCompatibilityVersion, scope);
                case BYTE_BUFFER -> serializeViaByteBuffer(handle, serializableObject, backwardCompatibilityVersion, scope);
            };
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> byte[] serializeViaDataOutput(SerializationHandle<T> handle, T serializableObject, Version backwardCompatibilityVersion, Scope scope) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(byteStream);

        handle.serialize(DataOutputSerializationAdapter.INSTANCE, output, serializableObject, backwardCompatibilityVersion, scope);

        output.close();
        byteStream.close();

        return byteStream.toByteArray();
    }

    private static <T> byte[] serializeViaByteBuffer(SerializationHandle<T> handle, T serializableObject, Version backwardCompatibilityVersion, Scope scope) throws IOException {
        int size = handle.estimateSize(serializableObject, backwardCompatibilityVersion, scope);
        ByteBuffer buffer = ByteBuffer.allocate(size);

        handle.serialize(ByteBufferSerializationAdapter.INSTANCE, buffer, serializableObject, backwardCompatibilityVersion, scope);

        return buffer.array();
    }

    private static <T> T deserialize(SerializationHandle<T> handle, byte[] bytes, SerializationStyle style) {
        try {
            return switch (style) {
                case DATA_OUTPUT -> deserializeViaDataInput(handle, bytes);
                case BYTE_BUFFER -> handle.deserialize(ByteBufferSerializationAdapter.INSTANCE, ByteBuffer.wrap(bytes));
            };
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> T deserializeViaDataInput(SerializationHandle<T> handle, byte[] bytes) throws IOException {
        try (DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return handle.deserialize(DataOutputSerializationAdapter.INSTANCE, inputStream);
        }
    }

}
