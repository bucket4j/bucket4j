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

package io.github.bucket4j.remote.multiversion;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.remote.Backend;
import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.RemoteCommand;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class MultiversionBackend<K extends Serializable> implements Backend<K> {

    private final Protocol desiredProtocol;

    protected MultiversionBackend(Protocol desiredProtocol) {
        if (desiredProtocol == null) {
            // TODO use specific exception
            throw new NullPointerException();
        }
        this.desiredProtocol = desiredProtocol;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        String commandAsJson = desiredProtocol.toJson(command);
        String resultAsJson = execute(key, commandAsJson);
        return desiredProtocol.parseResult(resultAsJson, command);
    }

    @Override
    public void createInitialState(K key, BucketConfiguration configuration) {
        String configurationAsJson = desiredProtocol.toJson(configuration);
        createInitialState(key, configurationAsJson);
    }

    @Override
    public <T extends Serializable> T createInitialStateAndExecute(K key, BucketConfiguration configuration, RemoteCommand<T> command) {
        String configurationAsJson = desiredProtocol.toJson(configuration);
        String commandAsJson = desiredProtocol.toJson(command);
        String resultAsJson = createInitialStateAndExecute(key, configurationAsJson, commandAsJson);
        CommandResult<T> result = desiredProtocol.parseResult(resultAsJson, command);
        return result.getData();
    }

    @Override
    public Optional<BucketConfiguration> getConfiguration(K key) {
        Optional<String> configurationAsJson = getConfigurationAsJson(key);
        return configurationAsJson.map(desiredProtocol::parseConfiguration);
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        String commandAsJson = desiredProtocol.toJson(command);
        return executeAsync(key, commandAsJson)
                .thenApply(resultAsJson -> desiredProtocol.parseResult(resultAsJson, command));
    }

    @Override
    public <T extends Serializable>  CompletableFuture<T> createInitialStateAndExecuteAsync(K key, BucketConfiguration configuration, RemoteCommand<T> command) {
        String configurationAsJson = desiredProtocol.toJson(configuration);
        String commandAsJson = desiredProtocol.toJson(command);
        return createInitialStateAndExecuteAsync(key, configurationAsJson, commandAsJson)
                .thenApply(resultAsJson -> desiredProtocol.parseResult(resultAsJson, command))
                .thenApply(CommandResult::getData);
    }

    protected abstract String execute(K key, String commandAsJson);

    protected abstract void createInitialState(K key, String configurationAsJson);

    protected abstract Optional<String> getConfigurationAsJson(K key);

    protected abstract CompletableFuture<String> createInitialStateAndExecuteAsync(K key, String configurationAsJson, String commandAsJson);

    protected abstract CompletableFuture<String> executeAsync(K key, String commandAsJson);

    protected abstract String createInitialStateAndExecute(K key, String configurationAsJson, String commandAsJson);

}
