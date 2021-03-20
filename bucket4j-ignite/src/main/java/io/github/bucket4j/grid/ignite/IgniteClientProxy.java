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
package io.github.bucket4j.grid.ignite;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridCommand;
import io.github.bucket4j.grid.GridProxy;
import org.apache.ignite.client.ClientCache;

public class IgniteClientProxy<K extends Serializable> implements GridProxy<K> {

    private final ClientCache<K, GridBucketState> cache;

    public IgniteClientProxy(ClientCache<K, GridBucketState> cache) {
        this.cache = cache;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, GridCommand<T> command) {
        while (true) {
            long currentTimeNanos = currentTimeNanos();
            GridBucketState gridBucketOldState = cache.get(key);
            if (gridBucketOldState == null) {
                return CommandResult.bucketNotFound();
            }
            GridBucketState gridBucketNewState = gridBucketOldState.deepCopy();

            T result = command.execute(gridBucketNewState, currentTimeNanos);
            if (command.isBucketStateModified()) {
                if (!cache.replace(key, gridBucketOldState, gridBucketNewState)) {
                    // CAS failed
                    continue;
                }
            }
            return CommandResult.success(result);
        }
    }

    @Override
    public void createInitialState(K key, BucketConfiguration configuration) {
        if (cache.containsKey(key)) {
            return;
        }
        long currentTimeNanos = currentTimeNanos();
        BucketState bucketState = BucketState.createInitialState(configuration, currentTimeNanos);
        GridBucketState gridBucketState = new GridBucketState(configuration, bucketState);
        cache.putIfAbsent(key, gridBucketState);
    }

    @Override
    public <T extends Serializable> T createInitialStateAndExecute(K key, BucketConfiguration configuration, GridCommand<T> command) {
        while (true) {
            boolean newStateCreated = false;
            long currentTimeNanos = currentTimeNanos();
            GridBucketState gridBucketOldState = cache.get(key);
            GridBucketState gridBucketNewState;
            if (gridBucketOldState == null) {
                BucketState bucketState = BucketState.createInitialState(configuration, currentTimeNanos);
                gridBucketNewState = new GridBucketState(configuration, bucketState);
                newStateCreated = true;
            } else {
                gridBucketNewState = gridBucketOldState.deepCopy();
            }

            T result = command.execute(gridBucketNewState, currentTimeNanos);
            if (newStateCreated) {
                if (!cache.putIfAbsent(key, gridBucketNewState)) {
                    continue;
                }
            } else if (command.isBucketStateModified()) {
                if (!cache.replace(key, gridBucketOldState, gridBucketNewState)) {
                    // CAS failed
                    continue;
                }
            }
            return result;
        }
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, GridCommand<T> command) {
        // async API will be supported since Apache Ignite 2.10 thin client
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Serializable> CompletableFuture<T> createInitialStateAndExecuteAsync(K key, BucketConfiguration configuration, GridCommand<T> command) {
        // async API will be supported since Apache Ignite 2.10 thin client
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<BucketConfiguration> getConfiguration(K key) {
        GridBucketState state = cache.get(key);
        if (state == null) {
            return Optional.empty();
        } else {
            return Optional.of(state.getConfiguration());
        }
    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }

    private long currentTimeNanos() {
        return System.currentTimeMillis() * 1_000_000;
    }

}
