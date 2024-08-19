/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2024 Vladimir Bukhtoyarov
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
package io.github.bucket4j.distributed.proxy;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketListener;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronization;

public class RemoteAsyncBucketBuilderView<K, KeyOld> implements RemoteAsyncBucketBuilder<K> {
    private final RemoteAsyncBucketBuilder<KeyOld> target;
    private final Function<K, KeyOld> mapper;

    public RemoteAsyncBucketBuilderView(RemoteAsyncBucketBuilder<KeyOld> target, Function<K, KeyOld> mapper) {
        this.target = target;
        this.mapper = mapper;
    }

    @Override
    public RemoteAsyncBucketBuilder<K> withSynchronization(BucketSynchronization bucketSynchronization) {
        target.withSynchronization(bucketSynchronization);
        return this;
    }

    @Override
    public RemoteAsyncBucketBuilder<K> withImplicitConfigurationReplacement(long desiredConfigurationVersion, TokensInheritanceStrategy tokensInheritanceStrategy) {
        target.withImplicitConfigurationReplacement(desiredConfigurationVersion, tokensInheritanceStrategy);
        return this;
    }

    @Override
    public RemoteAsyncBucketBuilder<K> withListener(BucketListener listener) {
        target.withListener(listener);
        return this;
    }

    @Override
    public AsyncBucketProxy build(K key, Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier) {
        return target.build(mapper.apply(key), configurationSupplier);
    }

    // To prevent nesting of anonymous class instances, directly map the original instance.
    @Override
    public <K2> RemoteAsyncBucketBuilder<K2> withMapper(Function<? super K2, ? extends K> innerMapper) {
        return target.withMapper(mapper.compose(innerMapper));
    }
}
