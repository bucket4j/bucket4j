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

package io.github.bucket4j.grid.coherence;


import com.tangosol.net.NamedCache;
import com.tangosol.util.processor.SingleEntryAsynchronousProcessor;
import io.github.bucket4j.*;
import io.github.bucket4j.grid.jcache.JCacheEntryProcessor;
import io.github.bucket4j.remote.Backend;
import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.RemoteBucketState;
import io.github.bucket4j.remote.RemoteCommand;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The extension of Bucket4j library addressed to support <a href="https://www.oracle.com/technetwork/middleware/coherence/overview/index.html">Oracle Coherence</a> in-memory computing platform.
 *
 * @param <K>
 */
public class CoherenceBackend<K extends Serializable> implements Backend<K> {

    private static final BackendOptions OPTIONS = new BackendOptions(true, MathType.ALL, MathType.INTEGER_64_BITS);

    private final NamedCache<K, RemoteBucketState> cache;
    private final TimeMeter clientClock;

    public CoherenceBackend(NamedCache<K, RemoteBucketState> cache) {
        this.cache = cache;
        this.clientClock = null;
    }

    CoherenceBackend(NamedCache<K, RemoteBucketState> cache, TimeMeter clientClock) {
        this.cache = cache;
        this.clientClock = Objects.requireNonNull(clientClock);
    }

    @Override
    public BackendOptions getOptions() {
        return OPTIONS;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.executeProcessor(command, getClientSideTimeNanos());
        return (CommandResult<T>) cache.invoke(key, adoptEntryProcessor(entryProcessor));
    }

    @Override
    public void createInitialState(K key, BucketConfiguration configuration) {
        JCacheEntryProcessor<K, Nothing> entryProcessor = JCacheEntryProcessor.initStateProcessor(configuration, getClientSideTimeNanos());
        cache.invoke(key, adoptEntryProcessor(entryProcessor));
    }

    @Override
    public <T extends Serializable> T createInitialStateAndExecute(K key, BucketConfiguration configuration, RemoteCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.initStateAndExecuteProcessor(command, configuration, getClientSideTimeNanos());
        CommandResult<T> result = (CommandResult<T>) cache.invoke(key, adoptEntryProcessor(entryProcessor));
        return result.getData();
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.executeProcessor(command, getClientSideTimeNanos());
        return invokeAsync(key, entryProcessor);
    }

    @Override
    public <T extends Serializable> CompletableFuture<T> createInitialStateAndExecuteAsync(K key, BucketConfiguration configuration, RemoteCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.initStateAndExecuteProcessor(command, configuration, getClientSideTimeNanos());
        CompletableFuture<CommandResult<T>> result = invokeAsync(key, entryProcessor);
        return result.thenApply(CommandResult::getData);
    }

    @Override
    public Optional<BucketConfiguration> getConfiguration(K key) {
        RemoteBucketState state = cache.get(key);
        if (state == null) {
            return Optional.empty();
        } else {
            return Optional.of(state.getConfiguration());
        }
    }

    private <T extends Serializable>  CoherenceEntryProcessorAdapter adoptEntryProcessor(final JCacheEntryProcessor<K, T> entryProcessor) {
        return new CoherenceEntryProcessorAdapter<>(entryProcessor);
    }

    private <T extends Serializable> CompletableFuture<CommandResult<T>> invokeAsync(K key, JCacheEntryProcessor<K, T> entryProcessor) {
        CompletableFuture<CommandResult<T>> future = new CompletableFuture<>();
        SingleEntryAsynchronousProcessor<K, RemoteBucketState, CommandResult<T>> asyncProcessor =
                new SingleEntryAsynchronousProcessor<K, RemoteBucketState, CommandResult<T>>(adoptEntryProcessor(entryProcessor)) {
            @Override
            public void onResult(Map.Entry<K, CommandResult<T>> entry) {
                super.onResult(entry);
                future.complete(entry.getValue());
            }
            @Override
            public void onException(Throwable error) {
                super.onException(error);
                future.completeExceptionally(error);
            }
        };
        cache.invoke(key, asyncProcessor);
        return future;
    }

    private Long getClientSideTimeNanos() {
        return clientClock == null? null : clientClock.currentTimeNanos();
    }

}
