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

package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.IMap;
import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import io.github.bucket4j.*;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.remote.*;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * The extension of Bucket4j library addressed to support <a href="https://hazelcast.com//">Hazelcast</a> in-memory data grid.
 */
public class HazelcastBackend<K extends Serializable> implements Backend<K> {

    private final IMap<K, RemoteBucketState> cache;

    public HazelcastBackend(IMap<K, RemoteBucketState> cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        HazelcastEntryProcessor<K, T> entryProcessor = new HazelcastEntryProcessor<>(command);
        return (CommandResult<T>) cache.executeOnKey(key, entryProcessor);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        HazelcastEntryProcessor<K, T> entryProcessor = new HazelcastEntryProcessor<>(command);
        CompletableFuture<CommandResult<T>> future = new CompletableFuture<>();
        cache.submitToKey(key, entryProcessor, new ExecutionCallback() {
            @Override
            public void onResponse(Object response) {
                future.complete((CommandResult<T>) response);
            }

            @Override
            public void onFailure(Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }


    private static class HazelcastEntryProcessor<K extends Serializable, T extends Serializable> implements EntryProcessor<K, RemoteBucketState> {

        private static final long serialVersionUID = 1L;

        private final RemoteCommand<T> command;
        private EntryBackupProcessor<K, RemoteBucketState> backupProcessor;

        public HazelcastEntryProcessor(RemoteCommand<T> command) {
            this.command = command;
        }

        @Override
        public Object process(Map.Entry<K, RemoteBucketState> entry) {
            HazelcastMutableEntryAdapter<K> entryAdapter = new HazelcastMutableEntryAdapter<>(entry);
            CommandResult<T> result = command.execute(entryAdapter, TimeMeter.SYSTEM_MILLISECONDS.currentTimeNanos());
            if (entryAdapter.modified) {
                RemoteBucketState state = entry.getValue();
                backupProcessor = new SimpleBackupProcessor<>(state);
            }
            return result;
        }

        @Override
        public EntryBackupProcessor<K, RemoteBucketState> getBackupProcessor() {
            return backupProcessor;
        }

    }

    private static class HazelcastMutableEntryAdapter<K> implements MutableBucketEntry {

        private final Map.Entry<K, RemoteBucketState> entry;
        private boolean modified;

        public HazelcastMutableEntryAdapter(Map.Entry<K, RemoteBucketState> entry) {
            this.entry = entry;
        }

        @Override
        public boolean exists() {
            return entry.getValue() != null;
        }

        @Override
        public void set(RemoteBucketState value) {
            entry.setValue(value);
            this.modified = true;
        }

        @Override
        public RemoteBucketState get() {
            return entry.getValue();
        }

    }

    private static class SimpleBackupProcessor<K> implements EntryBackupProcessor<K, RemoteBucketState> {

        private static final long serialVersionUID = 1L;

        private final RemoteBucketState state;

        private SimpleBackupProcessor(RemoteBucketState state) {
            this.state = state;
        }

        @Override
        public void processBackup(Map.Entry<K, RemoteBucketState> entry) {
            entry.setValue(state);
        }

    }


}
