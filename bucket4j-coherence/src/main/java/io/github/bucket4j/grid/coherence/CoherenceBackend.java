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
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;
import com.tangosol.util.processor.SingleEntryAsynchronousProcessor;
import io.github.bucket4j.*;
import io.github.bucket4j.distributed.proxy.AbstractBackend;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.remote.*;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * The extension of Bucket4j library addressed to support <a href="https://www.oracle.com/technetwork/middleware/coherence/overview/index.html">Oracle Coherence</a> in-memory computing platform.
 *
 * @param <K>
 */
public class CoherenceBackend<K extends Serializable> extends AbstractBackend<K> {

    private final NamedCache<K, RemoteBucketState> cache;

    public CoherenceBackend(NamedCache<K, RemoteBucketState> cache) {
        this.cache = cache;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        CoherenceProcessor<K, T> entryProcessor = new CoherenceProcessor<>(command);
        return cache.invoke(key, entryProcessor);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        CoherenceProcessor<K, T> entryProcessor = new CoherenceProcessor<>(command);
        CompletableFuture<CommandResult<T>> future = new CompletableFuture<>();
        SingleEntryAsynchronousProcessor<K, RemoteBucketState, CommandResult<T>> asyncProcessor =
                new SingleEntryAsynchronousProcessor<K, RemoteBucketState, CommandResult<T>>(entryProcessor) {
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


    private static class CoherenceProcessor<K extends Serializable, T extends Serializable> extends AbstractProcessor<K, RemoteBucketState, CommandResult<T>> {

        private static final long serialVersionUID = 1L;

        private final RemoteCommand<T> command;

        public CoherenceProcessor(RemoteCommand<T> command) {
            this.command = command;
        }

        @Override
        public CommandResult<T> process(InvocableMap.Entry<K, RemoteBucketState> entry) {
            CoherenceEntry<K> entryAdapter = new CoherenceEntry<>(entry);
            return command.execute(entryAdapter, TimeMeter.SYSTEM_MILLISECONDS.currentTimeNanos());
        }

    }


    private static class CoherenceEntry<K extends Serializable> implements MutableBucketEntry {

        private final Map.Entry<K, RemoteBucketState> entry;

        public CoherenceEntry(InvocableMap.Entry<K, RemoteBucketState> entry) {
            this.entry = entry;
        }

        @Override
        public boolean exists() {
            return entry.getValue() != null;
        }

        @Override
        public void set(RemoteBucketState value) {
            entry.setValue(value);
        }

        @Override
        public RemoteBucketState get() {
            return entry.getValue();
        }

    }


}
