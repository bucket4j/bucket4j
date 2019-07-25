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

package io.github.bucket4j.redis.lettuce;

import io.github.bucket4j.MathType;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.BackendOptions;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.redis.AbstractRedisBackend;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

// TODO javadocs
public class LettuceRedisBackend<K extends Serializable> extends AbstractRedisBackend<K> {

    private final Set<MathType> SUPPORTED_MATH_TYPES = new HashSet<MathType>() {{
        add(MathType.IEEE_754);
    }};
    private final BackendOptions OPTIONS = new BackendOptions(true, SUPPORTED_MATH_TYPES, MathType.IEEE_754);
    private RedisTemplate<K, RemoteBucketState> redis;

    public LettuceRedisBackend(RedisConnectionFactory connectionFactory) {
        this.redis = new RedisTemplate<>();
        this.redis.setEnableTransactionSupport(true);
        this.redis.setConnectionFactory(connectionFactory);
        this.redis.afterPropertiesSet();
    }

    @Override
    public BackendOptions getOptions() {
        return OPTIONS;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        CommandResult<T> result = null;
        boolean safe = false;
        while (!safe) {
            this.redis.watch(key);
            RedisMutableBucketEntry bucket = new RedisMutableBucketEntry(this.redis.boundValueOps(key));
            result = command.execute(bucket, TimeMeter.SYSTEM_MILLISECONDS.currentTimeNanos());
            this.redis.multi();
            bucket.realSet();
            List<?> redisResult = this.redis.exec();
            safe = !redisResult.isEmpty();
        }
        return result;
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(final K key, final RemoteCommand<T> command) {
        return CompletableFuture.supplyAsync(() -> execute(key, command));
    }

    private class RedisMutableBucketEntry implements MutableBucketEntry {

        RedisMutableBucketEntry(BoundValueOperations<K, RemoteBucketState> redisBucketRef) {
            this.bucketRef = redisBucketRef;
            this.bucket = bucketRef.get();
        }

        @Override
        public boolean exists() {
            return redis.hasKey(bucketRef.getKey());
        }

        @Override
        public void set(RemoteBucketState remoteBucketState) {
            bucket = remoteBucketState;
        }

        void realSet() {
            bucketRef.set(bucket);
        }

        @Override
        public RemoteBucketState get() {
            return bucket;
        }

        private BoundValueOperations<K, RemoteBucketState> bucketRef;
        private RemoteBucketState bucket;

    }

}
