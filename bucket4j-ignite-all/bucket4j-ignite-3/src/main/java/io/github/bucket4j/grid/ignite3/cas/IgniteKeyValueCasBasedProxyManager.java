/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2026 Vladimir Bukhtoyarov
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
package io.github.bucket4j.grid.ignite3.cas;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.ignite.table.KeyValueView;

import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.grid.ignite3.Bucket4jIgnite3;
import io.github.bucket4j.grid.ignite3.Bucket4jIgnite3.IgniteKeyValueCasBasedProxyManagerBuilder;

/**
 * The Apache Ignite 3.x specific implementation of the compare-and-swap based proxy manager,
 * built directly on top of {@link KeyValueView}, without executing any code on the server side.
 *
 * @param <K> type of key
 */
public class IgniteKeyValueCasBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    private final KeyValueView<K, byte[]> keyValueView;

    public IgniteKeyValueCasBasedProxyManager(IgniteKeyValueCasBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.keyValueView = builder.getKeyValueView();
    }

    /**
     * @deprecated use {@link Bucket4jIgnite3#casBasedBuilder(KeyValueView)}
     */
    @Deprecated
    public IgniteKeyValueCasBasedProxyManager(KeyValueView<K, byte[]> keyValueView) {
        this(Bucket4jIgnite3.INSTANCE.casBasedBuilder(keyValueView));
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData(Optional<Long> timeoutNanos) {
                byte[] persistedState = keyValueView.get(null, key);
                return Optional.ofNullable(persistedState);
            }
            @Override
            public boolean compareAndSwap(byte[] originalDataBytes, byte[] newDataBytes, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                if (originalDataBytes == null) {
                    return keyValueView.putIfAbsent(null, key, newDataBytes);
                }
                return keyValueView.replace(null, key, originalDataBytes, newDataBytes);
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        return new AsyncCompareAndSwapOperation() {
            @Override
            public CompletableFuture<Optional<byte[]>> getStateData(Optional<Long> timeoutNanos) {
                return keyValueView.getAsync(null, key).thenApply(Optional::ofNullable);
            }
            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalDataBytes, byte[] newDataBytes, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                if (originalDataBytes == null) {
                    return keyValueView.putIfAbsentAsync(null, key, newDataBytes);
                }
                return keyValueView.replaceAsync(null, key, originalDataBytes, newDataBytes);
            }
        };
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public void removeProxy(K key) {
        keyValueView.remove(null, key);
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        return keyValueView.removeAsync(null, key).thenApply(result -> null);
    }

}
