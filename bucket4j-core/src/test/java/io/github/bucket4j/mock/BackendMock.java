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

package io.github.bucket4j.mock;


import io.github.bucket4j.*;
import io.github.bucket4j.remote.Backend;
import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.RemoteBucketState;
import io.github.bucket4j.remote.RemoteCommand;

import java.io.*;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class BackendMock<K extends Serializable> implements Backend<K> {

    private static final BucketOptions OPTIONS = new BucketOptions(true, MathType.ALL, MathType.INTEGER_64_BITS);

    private final TimeMeter timeMeter;
    private RemoteBucketState state;
    private RuntimeException exception;

    public BackendMock(TimeMeter timeMeter) {
        this.timeMeter = Objects.requireNonNull(timeMeter);
    }

    public void setException(RuntimeException exception) {
        this.exception = exception;
    }

    @Override
    public BucketOptions getOptions() {
        return OPTIONS;
    }

    @Override
    public TimeMeter getClientSideClock() {
        return timeMeter;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        if (exception != null) {
            throw new RuntimeException();
        }
        if (state == null) {
            return CommandResult.bucketNotFound();
        }
        emulateSerialization(key);
        command = emulateSerialization(command);
        RemoteBucketState newState = emulateSerialization(state);
        T resultData = command.execute(newState);
        if (command.isBucketStateModified()) {
            state = newState;
        }
        resultData = emulateSerialization(resultData);
        return CommandResult.success(resultData);
    }

    @Override
    public void createInitialState(Serializable key, BucketConfiguration configuration) {
        if (exception != null) {
            throw new RuntimeException();
        }
        BucketState bucketState = BucketState.createInitialState(configuration, timeMeter.currentTimeNanos());
        this.state = new RemoteBucketState(configuration, bucketState);
    }

    @Override
    public Optional<BucketConfiguration> getConfiguration(Serializable key) {
        if (exception != null) {
            throw new RuntimeException();
        }
        return Optional.of(state.getConfiguration());
    }

    @Override
    public <T extends Serializable> T createInitialStateAndExecute(K key, BucketConfiguration configuration, RemoteCommand<T> command) {
        if (exception != null) {
            throw new RuntimeException();
        }
        createInitialState(key, configuration);
        return execute(key, command).getData();
    }

    @Override
    public <T extends Serializable> CompletableFuture<T> createInitialStateAndExecuteAsync(K key, BucketConfiguration configuration, RemoteCommand<T> command) {
        if (exception != null) {
            CompletableFuture future = new CompletableFuture();
            future.completeExceptionally(new RuntimeException());
            return future;
        }
        return CompletableFuture.completedFuture(createInitialStateAndExecute(key, configuration, command));
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        if (exception != null) {
            CompletableFuture future = new CompletableFuture();
            future.completeExceptionally(new RuntimeException());
            return future;
        }
        return CompletableFuture.completedFuture(execute(key, command));
    }

    private static <T> T emulateSerialization(T object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            byte[] bytes = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (T) ois.readObject();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
