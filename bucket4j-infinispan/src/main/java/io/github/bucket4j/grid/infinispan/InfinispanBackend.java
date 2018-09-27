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

package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.*;
import io.github.bucket4j.grid.jcache.JCacheEntryProcessor;
import io.github.bucket4j.remote.Backend;
import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.RemoteBucketState;
import io.github.bucket4j.remote.RemoteCommand;
import org.infinispan.commons.CacheException;
import org.infinispan.functional.EntryView;
import org.infinispan.functional.FunctionalMap.ReadWriteMap;
import org.infinispan.util.function.SerializableFunction;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * The extension of Bucket4j library addressed to support <a href="https://ignite.apache.org/">Apache ignite</a> in-memory computing platform.
 *
 * TODO fix javadocs
 * Use this extension only if you need in asynchronous API, else stay at {@link io.github.bucket4j.grid.jcache.JCache}
 */
public class InfinispanBackend<K extends Serializable> implements Backend<K> {

    private static final BucketOptions OPTIONS = new BucketOptions(true, MathType.ALL, MathType.INTEGER_64_BITS);

    private final ReadWriteMap<K, RemoteBucketState> readWriteMap;
    private final TimeMeter clientClock;

    // TODO javadocs
    public InfinispanBackend(ReadWriteMap<K, RemoteBucketState> readWriteMap) {
        this.readWriteMap = Objects.requireNonNull(readWriteMap);
        this.clientClock = null;
    }

    InfinispanBackend(ReadWriteMap<K, RemoteBucketState> readWriteMap, TimeMeter clientClock) {
        this.readWriteMap = Objects.requireNonNull(readWriteMap);
        this.clientClock = Objects.requireNonNull(clientClock);
    }

    @Override
    public BucketOptions getOptions() {
        return OPTIONS;
    }

    @Override
    public TimeMeter getClientSideClock() {
        return clientClock;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.executeProcessor(command);
        return invokeSync(key, entryProcessor);
    }

    @Override
    public void createInitialState(K key, BucketConfiguration configuration) {
        JCacheEntryProcessor<K, Nothing> entryProcessor = JCacheEntryProcessor.initStateProcessor(configuration, clientClock);
        invokeSync(key, entryProcessor);
    }

    @Override
    public <T extends Serializable> T createInitialStateAndExecute(K key, BucketConfiguration configuration, RemoteCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.initStateAndExecuteProcessor(command, configuration);
        CommandResult<T> result = invokeSync(key, entryProcessor);
        return result.getData();
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.executeProcessor(command);
        return invokeAsync(key, entryProcessor);
    }

    @Override
    public <T extends Serializable> CompletableFuture<T> createInitialStateAndExecuteAsync(K key, BucketConfiguration configuration, RemoteCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.initStateAndExecuteProcessor(command, configuration);
        CompletableFuture<CommandResult<T>> result = invokeAsync(key, entryProcessor);
        return result.thenApply(CommandResult::getData);
    }

    @Override
    public Optional<BucketConfiguration> getConfiguration(K key) {
        try {
            SerializableFunction<EntryView.ReadWriteEntryView<K, RemoteBucketState>, RemoteBucketState> findFunction =
                    (SerializableFunction<EntryView.ReadWriteEntryView<K, RemoteBucketState>, RemoteBucketState>)
                    entry -> entry.find().orElse(null);
            RemoteBucketState state = readWriteMap.eval(key, findFunction).get();
            if (state == null) {
                return Optional.empty();
            } else {
                return Optional.of(state.getConfiguration());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new CacheException(e);
        }
    }

    private <T extends Serializable> CommandResult<T> invokeSync(final K key, final JCacheEntryProcessor<K, T> entryProcessor) {
        try {
            return readWriteMap.eval(key, new SerializableFunctionAdapter<>(entryProcessor)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CacheException(e);
        }
    }

    private <T extends Serializable> CompletableFuture<CommandResult<T>> invokeAsync(final K key, final JCacheEntryProcessor<K, T> entryProcessor) {
        return readWriteMap.eval(key, new SerializableFunctionAdapter<>(entryProcessor));
    }

}
