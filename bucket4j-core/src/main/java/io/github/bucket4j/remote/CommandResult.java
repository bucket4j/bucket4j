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

package io.github.bucket4j.remote;

import java.io.Serializable;

public class CommandResult<T> implements Serializable {

    private static final long serialVersionUID = 42;

    private static final CommandResult<?> NOT_FOUND = new CommandResult<>(null, true);

    private T data;
    private boolean bucketNotFound;

    public CommandResult(T data, boolean bucketNotFound) {
        this.data = data;
        this.bucketNotFound = bucketNotFound;
    }

    public static <R> CommandResult<R> success(R data) {
        return new CommandResult<>(data, false);
    }

    public static <R> CommandResult<R> bucketNotFound() {
        return (CommandResult<R>) NOT_FOUND;
    }

    public T getData() {
        return data;
    }

    public boolean isBucketNotFound() {
        return bucketNotFound;
    }

}
