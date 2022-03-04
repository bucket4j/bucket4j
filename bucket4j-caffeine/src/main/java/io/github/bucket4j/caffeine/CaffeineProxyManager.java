/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
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

package io.github.bucket4j.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.Request;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The extension of Bucket4j library addressed to support <a href="https://hazelcast.com//">Hazelcast</a> in-memory data grid.
 */
public class CaffeineProxyManager<K> extends AbstractProxyManager<K> {

    private final Cache<K, RemoteBucketState> cache;

    /**
     * Creates new instance of {@link CaffeineProxyManager}
     *
     * @param builder the builder that will be used for cache creation
     * @param keepAfterRefillDuration specifies how long bucket should be held in the cache after all consumed tokens have been refilled.
     */
    public CaffeineProxyManager(Caffeine<K, RemoteBucketState> builder, Duration keepAfterRefillDuration) {
        this(builder, keepAfterRefillDuration, ClientSideConfig.getDefault());
    }

    public CaffeineProxyManager(Caffeine<K, RemoteBucketState> builder, Duration keepAfterRefillDuration, ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
        this.cache = builder
            .expireAfter(new Expiry<K, RemoteBucketState>() {
                @Override
                public long expireAfterCreate(K key, RemoteBucketState bucketState, long currentTime) {
                    long currentTimeNanos = getCurrentTime(clientSideConfig);
                    long nanosToFullRefill = bucketState.calculateFullRefillingTime(currentTimeNanos);
                    return nanosToFullRefill + keepAfterRefillDuration.toNanos();
                }

                @Override
                public long expireAfterUpdate(K key, RemoteBucketState bucketState, long currentTime, long currentDuration) {
                    long currentTimeNanos = getCurrentTime(clientSideConfig);
                    long nanosToFullRefill = bucketState.calculateFullRefillingTime(currentTimeNanos);
                    return nanosToFullRefill + keepAfterRefillDuration.toNanos();
                }

                @Override
                public long expireAfterRead(K key, RemoteBucketState bucketState, long currentTime, long currentDuration) {
                    long currentTimeNanos = getCurrentTime(clientSideConfig);
                    long nanosToFullRefill = bucketState.calculateFullRefillingTime(currentTimeNanos);
                    return nanosToFullRefill + keepAfterRefillDuration.toNanos();
                }
            })
            .build();
    }

    /**
     * Returns the cache that is used for storing the buckets
     *
     * @return the cache that is used for storing the buckets
     */
    public Cache<K, RemoteBucketState> getCache() {
        return cache;
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        CommandResult<T>[] resultHolder = new CommandResult[1];

        cache.asMap().compute(key, (K k, RemoteBucketState previousState) -> {
            Long clientSideTime = request.getClientSideTime();
            long timeNanos = clientSideTime != null ? clientSideTime : System.currentTimeMillis() * 1_000_000;
            RemoteBucketState[] stateHolder = new RemoteBucketState[] {
                previousState == null ? null : previousState.copy()
            };
            MutableBucketEntry entry = new MutableBucketEntry() {
                @Override
                public boolean exists() {
                    return stateHolder[0] != null;
                }
                @Override
                public void set(RemoteBucketState state) {
                    stateHolder[0] = state;
                }
                @Override
                public RemoteBucketState get() {
                    return stateHolder[0];
                }
            };
            resultHolder[0] = request.getCommand().execute(entry, timeNanos);
            return stateHolder[0];
        });

        return resultHolder[0];
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        CommandResult<T> result = execute(key, request);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public void removeProxy(K key) {
        cache.asMap().remove(key);
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        cache.asMap().remove(key);
        return CompletableFuture.completedFuture(null);
    }

    private static long getCurrentTime(ClientSideConfig clientSideConfig) {
        Optional<TimeMeter> clock = clientSideConfig.getClientSideClock();
        return clock.isPresent() ? clock.get().currentTimeNanos() : System.currentTimeMillis() * 1_000_000;
    }

}
