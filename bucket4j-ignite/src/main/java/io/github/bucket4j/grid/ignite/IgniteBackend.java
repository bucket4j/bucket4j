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
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.remote.*;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteInClosure;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;


/**
 * The extension of Bucket4j library addressed to support <a href="https://ignite.apache.org/">Apache ignite</a> in-memory computing platform.
 */
public class IgniteBackend<K extends Serializable> implements Backend<K> {

    private static final BackendOptions OPTIONS = new BackendOptions(true, MathType.ALL, MathType.INTEGER_64_BITS);

    private final IgniteCache<K, RemoteBucketState> cache;

    public IgniteBackend(IgniteCache<K, RemoteBucketState> cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    @Override
    public BackendOptions getOptions() {
        return OPTIONS;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        IgniteProcessor<K, T> entryProcessor = new IgniteProcessor<>(command);
        return cache.invoke(key, entryProcessor);
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        IgniteProcessor<K, T> entryProcessor = new IgniteProcessor<>(command);
        IgniteFuture<CommandResult<T>> igniteFuture = cache.invokeAsync(key, entryProcessor);
        CompletableFuture<CommandResult<T>> completableFuture = new CompletableFuture<>();
        igniteFuture.listen((IgniteInClosure<IgniteFuture<CommandResult<T>>>) completedIgniteFuture -> {
            try {
                completableFuture.complete(completedIgniteFuture.get());
            } catch (Throwable t) {
                completableFuture.completeExceptionally(t);
            }
        });
        return completableFuture;
    }


    private static class IgniteProcessor<K extends Serializable, T extends Serializable> implements Serializable,
            CacheEntryProcessor<K, RemoteBucketState, CommandResult<T>> {

        private static final long serialVersionUID = 1;

        private final RemoteCommand<T> command;

        private IgniteProcessor(RemoteCommand<T> command) {
            this.command = command;
        }

        @Override
        public CommandResult<T> process(MutableEntry<K, RemoteBucketState> entry, Object... arguments) throws EntryProcessorException {
            return command.execute(new IgniteEntry(entry), TimeMeter.SYSTEM_MILLISECONDS.currentTimeNanos());
        }

    }

    private static class IgniteEntry implements MutableBucketEntry {

        private final MutableEntry<?, RemoteBucketState> entry;

        private IgniteEntry(MutableEntry<?, RemoteBucketState> entry) {
            this.entry = entry;
        }

        @Override
        public boolean exists() {
            return entry.exists();
        }

        @Override
        public void set(RemoteBucketState state) {
            entry.setValue(state);
        }

        @Override
        public RemoteBucketState get() {
            return entry.getValue();
        }

    }

}
