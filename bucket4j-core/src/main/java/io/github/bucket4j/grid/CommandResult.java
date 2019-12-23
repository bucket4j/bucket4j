/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.grid;

import io.github.bucket4j.serialization.DeserializationBinding;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.serialization.SerializationBinding;

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
        public <S> CommandResult<?> deserialize(DeserializationBinding<S> binding, S source) throws IOException {
            boolean isBucketNotFound = binding.readBoolean(source);
            return isBucketNotFound
                    ? CommandResult.bucketNotFound()
                    : CommandResult.success((Serializable) binding.readObject(source));
        }

        @Override
        public <O> void serialize(SerializationBinding<O> binding, O target, CommandResult<?> result) throws IOException {
            binding.writeBoolean(target, result.bucketNotFound);
            if (!result.bucketNotFound) {
                binding.writeObject(target, result.data);
            }
        }
    };

}
