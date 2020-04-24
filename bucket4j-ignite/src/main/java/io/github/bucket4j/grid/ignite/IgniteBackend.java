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
import io.github.bucket4j.distributed.proxy.AbstractBackend;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.serialization.InternalSerializationHelper;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteInClosure;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static io.github.bucket4j.serialization.InternalSerializationHelper.*;


/**
 * The extension of Bucket4j library addressed to support <a href="https://ignite.apache.org/">Apache ignite</a> in-memory computing platform.
 */
public class IgniteBackend<K> extends AbstractBackend<K> {

    private final IgniteCache<K, RemoteBucketState> cache;

    public IgniteBackend(IgniteCache<K, RemoteBucketState> cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    @Override
    public <T> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        IgniteProcessor<K> entryProcessor = new IgniteProcessor<>(command);
        byte[] resultBytes = cache.invoke(key, entryProcessor);
        return deserializeResult(resultBytes);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        IgniteProcessor<K> entryProcessor = new IgniteProcessor<>(command);
        IgniteFuture<byte[]> igniteFuture = cache.invokeAsync(key, entryProcessor);
        CompletableFuture<CommandResult<T>> completableFuture = new CompletableFuture<>();
        igniteFuture.listen((IgniteInClosure<IgniteFuture<byte[]>>) completedIgniteFuture -> {
            try {
                byte[] resultBytes = completedIgniteFuture.get();
                CommandResult<T> result = deserializeResult(resultBytes);
                completableFuture.complete(result);
            } catch (Throwable t) {
                completableFuture.completeExceptionally(t);
            }
        });
        return completableFuture;
    }


    private static class IgniteProcessor<K> implements Serializable, CacheEntryProcessor<K, RemoteBucketState, byte[]> {

        private static final long serialVersionUID = 1;

        private final byte[] commandBytes;

        private IgniteProcessor(RemoteCommand<?> command) {
            this.commandBytes = serializeCommand(command);
        }

        @Override
        public byte[] process(MutableEntry<K, RemoteBucketState> entry, Object... arguments) throws EntryProcessorException {
            RemoteCommand<?> command = deserializeCommand(commandBytes);
            CommandResult<?> result = command.execute(new IgniteEntry(entry), TimeMeter.SYSTEM_MILLISECONDS.currentTimeNanos());
            return serializeResult(result);
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
