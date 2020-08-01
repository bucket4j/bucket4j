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
import com.tangosol.util.processor.SingleEntryAsynchronousProcessor;
import io.github.bucket4j.distributed.proxy.AbstractBackend;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.*;


/**
 * The extension of Bucket4j library addressed to support <a href="https://www.oracle.com/technetwork/middleware/coherence/overview/index.html">Oracle Coherence</a> in-memory computing platform.
 *
 * @param <K>
 */
public class CoherenceBackend<K> extends AbstractBackend<K> {

    private final NamedCache<K, byte[]> cache;

    public CoherenceBackend(NamedCache<K, byte[]> cache) {
        this.cache = cache;
    }

    @Override
    public <T> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        CoherenceProcessor<K, T> entryProcessor = new CoherenceProcessor<>(command);
        byte[] resultBytes = cache.invoke(key, entryProcessor);
        return deserializeResult(resultBytes);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        CoherenceProcessor<K, T> entryProcessor = new CoherenceProcessor<>(command);
        CompletableFuture<CommandResult<T>> future = new CompletableFuture<>();
        SingleEntryAsynchronousProcessor<K, byte[], byte[]> asyncProcessor =
            new SingleEntryAsynchronousProcessor<K, byte[], byte[]>(entryProcessor) {
                @Override
                public void onResult(Map.Entry<K, byte[]> entry) {
                    super.onResult(entry);
                    byte[] resultBytes = entry.getValue();
                    future.complete(deserializeResult(resultBytes));
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


    public static class CoherenceEntry<K> implements MutableBucketEntry {

        private final Map.Entry<K, byte[]> entry;

        public CoherenceEntry(InvocableMap.Entry<K, byte[]> entry) {
            this.entry = entry;
        }

        @Override
        public boolean exists() {
            return entry.getValue() != null;
        }

        @Override
        public void set(RemoteBucketState value) {
            byte[] stateBytes = serializeState(value);
            entry.setValue(stateBytes);
        }

        @Override
        public RemoteBucketState get() {
            byte[] stateBytes = entry.getValue();
            return deserializeState(stateBytes);
        }

    }

}
