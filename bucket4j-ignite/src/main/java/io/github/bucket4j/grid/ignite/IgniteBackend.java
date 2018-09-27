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

package io.github.bucket4j.grid.ignite;

import io.github.bucket4j.*;
import io.github.bucket4j.grid.jcache.JCacheEntryProcessor;
import io.github.bucket4j.remote.Backend;
import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.RemoteBucketState;
import io.github.bucket4j.remote.RemoteCommand;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteInClosure;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


/**
 * The extension of Bucket4j library addressed to support <a href="https://ignite.apache.org/">Apache ignite</a> in-memory computing platform.
 *
 * Use this extension only if you need in asynchronous API, else stay at {@link io.github.bucket4j.grid.jcache.JCache}
 */
public class IgniteBackend<K extends Serializable> implements Backend<K> {

    private static final BucketOptions OPTIONS = new BucketOptions(true, MathType.ALL, MathType.INTEGER_64_BITS);

    private final IgniteCache<K, RemoteBucketState> cache;
    private final TimeMeter clientClock;

    public IgniteBackend(IgniteCache<K, RemoteBucketState> cache) {
        this.cache = Objects.requireNonNull(cache);
        this.clientClock = null;
    }

    IgniteBackend(IgniteCache<K, RemoteBucketState> cache, TimeMeter clientClock) {
        this.cache = Objects.requireNonNull(cache);
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
        return cache.invoke(key, entryProcessor);
    }

    @Override
    public void createInitialState(K key, BucketConfiguration configuration) {
        JCacheEntryProcessor<K, Nothing> entryProcessor = JCacheEntryProcessor.initStateProcessor(configuration, clientClock);
        cache.invoke(key, entryProcessor);
    }

    @Override
    public <T extends Serializable> T createInitialStateAndExecute(K key, BucketConfiguration configuration, RemoteCommand<T> command) {
        JCacheEntryProcessor<K, T> entryProcessor = JCacheEntryProcessor.initStateAndExecuteProcessor(command, configuration);
        CommandResult<T> result = cache.invoke(key, entryProcessor);
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
        RemoteBucketState state = cache.get(key);
        if (state == null) {
            return Optional.empty();
        } else {
            return Optional.of(state.getConfiguration());
        }
    }

    private <T extends Serializable> CompletableFuture<CommandResult<T>> invokeAsync(K key, JCacheEntryProcessor<K, T> entryProcessor) {
        return convertFuture(cache.invokeAsync(key, entryProcessor));
    }

    private static <T> CompletableFuture<T> convertFuture(IgniteFuture<T> igniteFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        igniteFuture.listen((IgniteInClosure<IgniteFuture<T>>) completedIgniteFuture -> {
            try {
                completableFuture.complete(completedIgniteFuture.get());
            } catch (Throwable t) {
                completableFuture.completeExceptionally(t);
            }
        });
        return completableFuture;
    }

}
