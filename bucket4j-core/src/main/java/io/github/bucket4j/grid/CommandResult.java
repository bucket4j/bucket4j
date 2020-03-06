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

package io.github.bucket4j.grid;

import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.serialization.SerializationAdapter;

import java.io.IOException;
import java.io.Serializable;

public class CommandResult<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final CommandResult<?> NOT_FOUND = new CommandResult<>(null, true);

    private T data;
    private boolean bucketNotFound;

    public CommandResult(T data, boolean bucketNotFound) {
        this.data = data;
        this.bucketNotFound = bucketNotFound;
    }

    public static <R extends Serializable> CommandResult<R> success(R data) {
        return new CommandResult<>(data, false);
    }

    public static <R extends Serializable> CommandResult<R> bucketNotFound() {
        return (CommandResult<R>) NOT_FOUND;
    }

    public T getData() {
        return data;
    }

    public boolean isBucketNotFound() {
        return bucketNotFound;
    }

    public static SerializationHandle<CommandResult<?>> SERIALIZATION_HANDLE = new SerializationHandle<CommandResult<?>>() {
        @Override
        public <S> CommandResult<?> deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            boolean isBucketNotFound = adapter.readBoolean(input);
            return isBucketNotFound
                    ? CommandResult.bucketNotFound()
                    : CommandResult.success((Serializable) adapter.readObject(input));
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, CommandResult<?> result) throws IOException {
            adapter.writeBoolean(output, result.bucketNotFound);
            if (!result.bucketNotFound) {
                adapter.writeObject(output, result.data);
            }
        }

        @Override
        public int getTypeId() {
            return 14;
        }

        @Override
        public Class<CommandResult<?>> getSerializedType() {
            return (Class) CommandResult.class;
        }
    };

}
