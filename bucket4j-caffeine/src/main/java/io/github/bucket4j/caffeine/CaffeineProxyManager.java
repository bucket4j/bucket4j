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

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.Request;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * The extension of Bucket4j library addressed to support <a href="https://github.com/ben-manes/caffeine">Caffeine</a> caching library.
 */
public class CaffeineProxyManager<K> extends AbstractProxyManager<K> {

    private final Cache<K, RemoteBucketState> cache;

    CaffeineProxyManager(Bucket4jCaffeine.CaffeineProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());

        this.cache = builder.cacheBuilder.expireAfter(new Expiry<K, RemoteBucketState>() {
                private final ExpirationAfterWriteStrategy expiration = getClientSideConfig().getExpirationAfterWriteStrategy()
                    .orElse(ExpirationAfterWriteStrategy.none());

                @Override
                public long expireAfterCreate(K key, RemoteBucketState bucketState, long currentTime) {
                    long ttlNanos = expiration.calculateTimeToLiveMillis(bucketState, currentTimeNanos());
                    return ttlNanos < 0 ? Long.MAX_VALUE : ttlNanos;
                }

                @Override
                public long expireAfterUpdate(K key, RemoteBucketState bucketState, long currentTime, long currentDuration) {
                    long ttlNanos = expiration.calculateTimeToLiveMillis(bucketState, currentTimeNanos());
                    return ttlNanos < 0 ? Long.MAX_VALUE : ttlNanos;
                }

                @Override
                public long expireAfterRead(K key, RemoteBucketState bucketState, long currentTime, long currentDuration) {
                    long ttlNanos = expiration.calculateTimeToLiveMillis(bucketState, currentTimeNanos());
                    return ttlNanos < 0 ? Long.MAX_VALUE : ttlNanos;
                }
            })
            .build();
    }

    /**
     * @deprecated use {@link Bucket4jCaffeine#builderFor(Caffeine)}
     */
    @Deprecated
    public CaffeineProxyManager(Caffeine<? super K, ? super RemoteBucketState> builder, Duration keepAfterRefillDuration) {
        this(builder, keepAfterRefillDuration, ClientSideConfig.getDefault());
    }

    /**
     * @deprecated use {@link Bucket4jCaffeine#builderFor(Caffeine)}
     */
    @Deprecated
    public CaffeineProxyManager(Caffeine<? super K, ? super RemoteBucketState> builder, Duration keepAfterRefillDuration, ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
        this.cache = builder
            .expireAfter(new Expiry<K, RemoteBucketState>() {
                @Override
                public long expireAfterCreate(K key, RemoteBucketState bucketState, long currentTime) {
                    long currentTimeNanos = currentTimeNanos();
                    long nanosToFullRefill = bucketState.calculateFullRefillingTime(currentTimeNanos);
                    return nanosToFullRefill + keepAfterRefillDuration.toNanos();
                }

                @Override
                public long expireAfterUpdate(K key, RemoteBucketState bucketState, long currentTime, long currentDuration) {
                    long currentTimeNanos = currentTimeNanos();
                    long nanosToFullRefill = bucketState.calculateFullRefillingTime(currentTimeNanos);
                    return nanosToFullRefill + keepAfterRefillDuration.toNanos();
                }

                @Override
                public long expireAfterRead(K key, RemoteBucketState bucketState, long currentTime, long currentDuration) {
                    long currentTimeNanos = currentTimeNanos();
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
            MutableBucketEntry entryWrapper = new MutableBucketEntry(previousState == null ? null : previousState.copy());
            resultHolder[0] = request.getCommand().execute(entryWrapper, timeNanos);
            return entryWrapper.exists() ? entryWrapper.get() : null;
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

}