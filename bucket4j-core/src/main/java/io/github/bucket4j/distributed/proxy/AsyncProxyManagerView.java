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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.AsyncBucketProxy;

public class AsyncProxyManagerView<K, OldKey> implements AsyncProxyManager<K> {

    private final AsyncProxyManager<OldKey> target;
    private final Function<? super K, OldKey> mapper;

    public AsyncProxyManagerView(AsyncProxyManager<OldKey> target, Function<K, OldKey> mapper) {
        this.target = target;
        this.mapper = mapper;
    }

    @Override
    public AsyncBucketProxy getProxy(K key, Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier) {
        return target.getProxy(mapper.apply(key), configurationSupplier);
    }

    @Override
    public RemoteAsyncBucketBuilder<K> builder() {
        return target.builder().withMapper(mapper);
    }

    @Override
    public CompletableFuture<Void> removeProxy(K key) {
        return target.removeProxy(mapper.apply(key));
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return target.isExpireAfterWriteSupported();
    }

    @Override
    public CompletableFuture<Optional<BucketConfiguration>> getProxyConfiguration(K key) {
        return target.getProxyConfiguration(mapper.apply(key));
    }

    // To prevent nesting of anonymous class instances, directly map the original instance.
    @Override
    public <K2> AsyncProxyManager<K2> withMapper(Function<? super K2, ? extends K> innerMapper) {
        return target.withMapper(mapper.compose(innerMapper));
    }

}
