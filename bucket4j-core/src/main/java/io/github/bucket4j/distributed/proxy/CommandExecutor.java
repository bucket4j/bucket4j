/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;


// TODO javadocs
public interface CommandExecutor<K extends Serializable> {

    // TODO javadocs
    <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command);

    // TODO javadocs
    <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command);

    static <K extends Serializable> CommandExecutor<K> nonOptimized(final Backend<K> backend) {
        return new CommandExecutor<K>() {
            @Override
            public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
                return backend.execute(key, command);
            }

            @Override
            public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
                return backend.executeAsync(key, command);
            }
        };
    }

}
