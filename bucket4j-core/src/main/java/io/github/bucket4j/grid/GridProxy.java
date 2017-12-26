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

package io.github.bucket4j.grid;

import io.github.bucket4j.BucketConfiguration;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface GridProxy<K extends Serializable> {

    <T extends Serializable> CommandResult<T> execute(K key, GridCommand<T> command);

    void createInitialState(K key, BucketConfiguration configuration);

    <T extends Serializable> T createInitialStateAndExecute(K key, BucketConfiguration configuration, GridCommand<T> command);

    <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, GridCommand<T> command);

    <T extends Serializable> CompletableFuture<T> createInitialStateAndExecuteAsync(K key, BucketConfiguration configuration, GridCommand<T> command);

    Optional<BucketConfiguration> getConfiguration(K key);

    boolean isAsyncModeSupported();

}
