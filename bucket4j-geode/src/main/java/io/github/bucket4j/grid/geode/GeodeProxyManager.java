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
package io.github.bucket4j.grid.geode;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.geode.cache.CacheTransactionManager;
import org.apache.geode.cache.CommitConflictException;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;

import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

/**
 * The extension of Bucket4j library addressed to support <a href="https://geode.apache.org/">Apache Geode (GemFire)</a> in-memory data grid.
 *
 * <p>{@link Region} inherits {@link java.util.concurrent.ConcurrentMap}, but its {@code replace(key, oldValue, newValue)}
 * cannot be trusted as a conflict check for {@code byte[]} values: {@code byte[]} uses identity {@code equals},
 * and Geode is free to hand back a freshly deserialized array on every {@code get} (this is guaranteed to happen
 * whenever the entry has ever crossed the wire, and is not something client code can rely on even for
 * single-member setups). Instead, every compare-and-swap attempt is wrapped in a Geode
 * {@link CacheTransactionManager cache transaction}: the current value is compared against the expected one
 * <em>after</em> the transaction has started, and {@link CommitConflictException} on commit reveals whether
 * another member modified the entry concurrently - which is exactly the semantics {@link CompareAndSwapOperation}
 * needs.
 *
 * @param <K> type of the key
 */
public class GeodeProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    private final Region<K, byte[]> region;
    private final CacheTransactionManager transactionManager;

    GeodeProxyManager(Bucket4jGeode.GeodeProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.region = builder.region;
        this.transactionManager = ((GemFireCache) region.getRegionService()).getCacheTransactionManager();
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData(Optional<Long> timeoutNanos) {
                return Optional.ofNullable(region.get(key));
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                transactionManager.begin();
                try {
                    if (!Arrays.equals(region.get(key), originalData)) {
                        return false;
                    }
                    region.put(key, newData);
                    transactionManager.commit();
                    return true;
                } catch (CommitConflictException e) {
                    return false;
                } finally {
                    // commit() already rolls back internally on CommitConflictException, so exists() guards
                    // against calling rollback() on a transaction that Geode already terminated
                    if (transactionManager.exists()) {
                        transactionManager.rollback();
                    }
                }
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }

    @Override
    public void removeProxy(K key) {
        region.remove(key);
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        throw new UnsupportedOperationException();
    }

}
