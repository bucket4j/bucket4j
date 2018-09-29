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

package io.github.bucket4j.redis;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketOptions;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.remote.Backend;
import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.RemoteCommand;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

// TODO javadocs
public abstract class AbstractRedisBackend<K extends Serializable> implements Backend<K> {

    @Override
    public BucketOptions getOptions() {
        return null;
    }

    @Override
    public TimeMeter getClientSideClock() {
        return null;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        return null;
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        return null;
    }

    @Override
    public void createInitialState(K key, BucketConfiguration configuration) {
        // TODO
    }

    @Override
    public <T extends Serializable> T createInitialStateAndExecute(K key, BucketConfiguration configuration, RemoteCommand<T> command) {
        return null;
    }

    @Override
    public <T extends Serializable> CompletableFuture<T> createInitialStateAndExecuteAsync(K key, BucketConfiguration configuration, RemoteCommand<T> command) {
        return null;
    }

    @Override
    public Optional<BucketConfiguration> getConfiguration(K key) {
        return Optional.empty();
    }

}
