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

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketOptions;
import io.github.bucket4j.MathType;
import io.github.bucket4j.TimeMeter;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an extension point of bucket4j library.
 *
 * TODO javadocs
 *
 * @param <K>
 */
public interface Backend<K extends Serializable> {

    // TODO javadocs
    BucketOptions getOptions();

    // TODO javadocs
    TimeMeter getClientSideClock();

    // TODO javadocs
    <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command);

    // TODO javadocs
    <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command);

    // TODO javadocs
    void createInitialState(K key, BucketConfiguration configuration);

    // TODO javadocs
    <T extends Serializable> T createInitialStateAndExecute(K key, BucketConfiguration configuration, RemoteCommand<T> command);

    // TODO javadocs
    <T extends Serializable> CompletableFuture<T> createInitialStateAndExecuteAsync(K key, BucketConfiguration configuration, RemoteCommand<T> command);

    // TODO javadocs
    Optional<BucketConfiguration> getConfiguration(K key);

}
