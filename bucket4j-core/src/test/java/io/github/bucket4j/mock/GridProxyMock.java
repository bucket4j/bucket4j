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

package io.github.bucket4j.mock;


import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridCommand;
import io.github.bucket4j.grid.GridProxy;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class GridProxyMock implements GridProxy {

    private final TimeMeter timeMeter;
    private GridBucketState state;
    private RuntimeException exception;

    public GridProxyMock(TimeMeter timeMeter) {
        this.timeMeter = timeMeter;
    }

    public void setException(RuntimeException exception) {
        this.exception = exception;
    }

    @Override
    public CommandResult execute(Serializable key, GridCommand command) {
        if (exception != null) {
            throw new RuntimeException();
        }
        if (state == null) {
            return CommandResult.bucketNotFound();
        }
        emulateSerialization(key);
        command = emulateSerialization(command);
        GridBucketState newState = emulateSerialization(state);
        Serializable resultData = command.execute(newState, timeMeter.currentTimeNanos());
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
        this.state = new GridBucketState(configuration, bucketState);
    }

    @Override
    public Optional<BucketConfiguration> getConfiguration(Serializable key) {
        if (exception != null) {
            throw new RuntimeException();
        }
        return Optional.of(state.getConfiguration());
    }

    @Override
    public Serializable createInitialStateAndExecute(Serializable key, BucketConfiguration configuration, GridCommand command) {
        if (exception != null) {
            throw new RuntimeException();
        }
        createInitialState(key, configuration);
        return execute(key, command);
    }

    @Override
    public CompletableFuture createInitialStateAndExecuteAsync(Serializable key, BucketConfiguration configuration, GridCommand command) {
        if (exception != null) {
            CompletableFuture future = new CompletableFuture();
            future.completeExceptionally(new RuntimeException());
            return future;
        }
        return CompletableFuture.completedFuture(createInitialStateAndExecute(key, configuration, command));
    }

    @Override
    public CompletableFuture<CommandResult> executeAsync(Serializable key, GridCommand command) {
        if (exception != null) {
            CompletableFuture future = new CompletableFuture();
            future.completeExceptionally(new RuntimeException());
            return future;
        }
        return CompletableFuture.completedFuture(execute(key, command));
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
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
