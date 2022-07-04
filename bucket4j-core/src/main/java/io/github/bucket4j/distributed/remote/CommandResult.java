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

import io.github.bucket4j.Nothing;

import io.github.bucket4j.distributed.serialization.*;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.github.bucket4j.distributed.serialization.PrimitiveSerializationHandles.*;
import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class CommandResult<T> implements ComparableByContent<CommandResult> {

    public static final CommandResult<Nothing> NOTHING = CommandResult.success(Nothing.INSTANCE, NULL_HANDLE);
    public static final CommandResult<Long> ZERO = CommandResult.success(0L, LONG_HANDLE);
    public static final CommandResult<Long> MAX_VALUE = CommandResult.success(Long.MAX_VALUE, LONG_HANDLE);
    public static final CommandResult<Boolean> TRUE = CommandResult.success(true, BOOLEAN_HANDLE);
    public static final CommandResult<Boolean> FALSE = CommandResult.success(false, BOOLEAN_HANDLE);

    private static final CommandResult<?> NOT_FOUND = new CommandResult<>(new BucketNotFoundError(), BucketNotFoundError.SERIALIZATION_HANDLE.getTypeId());
    private static final CommandResult<?> NULL = new CommandResult<>(null, NULL_HANDLE.getTypeId());

    private T data;
    private int resultTypeId;

    public static SerializationHandle<CommandResult<?>> SERIALIZATION_HANDLE = new SerializationHandle<CommandResult<?>>() {
        @Override
        public <S> CommandResult<?> deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            int typeId = adapter.readInt(input);
            SerializationHandle handle = SerializationHandles.CORE_HANDLES.getHandleByTypeId(typeId);
            Object resultData = handle.deserialize(adapter, input);

            return CommandResult.success(resultData, typeId);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, CommandResult<?> result, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            adapter.writeInt(output, result.resultTypeId);
            SerializationHandle handle = SerializationHandles.CORE_HANDLES.getHandleByTypeId(result.resultTypeId);
            handle.serialize(adapter, output, result.data, backwardCompatibilityVersion, scope);
        }

        @Override
        public int getTypeId() {
            return 10;
        }

        @Override
        public Class<CommandResult<?>> getSerializedType() {
            return (Class) CommandResult.class;
        }

        @Override
        public CommandResult<?> fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            Map<String, Object> dataSnapshot = (Map<String, Object>) snapshot.get("data");
            String typeName = (String) dataSnapshot.get("type");
            SerializationHandle handle = SerializationHandles.CORE_HANDLES.getHandleByTypeName(typeName);
            Object resultData = handle.fromJsonCompatibleSnapshot(dataSnapshot);

            return CommandResult.success(resultData, handle.getTypeId());
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(CommandResult<?> result, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("version", v_7_0_0.getNumber());
            SerializationHandle<Object> handle = SerializationHandles.CORE_HANDLES.getHandleByTypeId(result.resultTypeId);
            Map<String, Object> valueSnapshot = handle.toJsonCompatibleSnapshot(result.data, backwardCompatibilityVersion, scope);
            valueSnapshot.put("type", handle.getTypeName());
            snapshot.put("data", valueSnapshot);
            return snapshot;
        }

        @Override
        public String getTypeName() {
            return "CommandResult";
        }

    };

    public CommandResult(T data, int resultTypeId) {
        this.data = data;
        this.resultTypeId = resultTypeId;
    }

    public static <R> CommandResult<R> success(R data, SerializationHandle dataSerializer) {
        return new CommandResult<>(data, dataSerializer.getTypeId());
    }

    public static <R> CommandResult<R> success(R data, int resultTypeId) {
        return new CommandResult<>(data, resultTypeId);
    }

    public static <R> CommandResult<R> bucketNotFound() {
        return (CommandResult<R>) NOT_FOUND;
    }

    public static <R> CommandResult<R> empty() {
        return (CommandResult<R>) NULL;
    }

    public static CommandResult<?> unsupportedType(int typeId) {
        UnsupportedTypeError error = new UnsupportedTypeError(typeId);
        return new CommandResult<>(error, UnsupportedTypeError.SERIALIZATION_HANDLE.getTypeId());
    }

    public static CommandResult<?> unsupportedNamedType(String typeName) {
        UnsupportedNamedTypeError error = new UnsupportedNamedTypeError(typeName);
        return new CommandResult<>(error, UnsupportedNamedTypeError.SERIALIZATION_HANDLE.getTypeId());
    }

    public static CommandResult<?> usageOfUnsupportedApiException(int requestedFormatNumber, int maxSupportedFormatNumber) {
        UsageOfUnsupportedApiError error = new UsageOfUnsupportedApiError(requestedFormatNumber, maxSupportedFormatNumber);
        return new CommandResult<>(error, UsageOfUnsupportedApiError.SERIALIZATION_HANDLE.getTypeId());
    }

    public static CommandResult<?> usageOfObsoleteApiException(int requestedFormatNumber, int minSupportedFormatNumber) {
        UsageOfObsoleteApiError error = new UsageOfObsoleteApiError(requestedFormatNumber, minSupportedFormatNumber);
        return new CommandResult<>(error, UsageOfObsoleteApiError.SERIALIZATION_HANDLE.getTypeId());
    }

    public T getData() {
        if (data instanceof CommandError) {
            CommandError error = (CommandError) data;
            throw error.asException();
        }
        return data;
    }

    public boolean isBucketNotFound() {
        return data instanceof BucketNotFoundError;
    }

    public int getResultTypeId() {
        return resultTypeId;
    }

    @Override
    public boolean equalsByContent(CommandResult other) {
        return resultTypeId == other.resultTypeId &&
                ComparableByContent.equals(data, other.data);
    }

}
