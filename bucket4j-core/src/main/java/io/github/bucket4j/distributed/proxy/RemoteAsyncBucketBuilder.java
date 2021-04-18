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

package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.proxy.optimization.Optimization;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * The builder for {@link AsyncBucketProxy}
 *
 * @param <K>
 */
public interface RemoteAsyncBucketBuilder<K> {

    /**
     * Configures custom recovery strategy instead of {@link RecoveryStrategy#RECONSTRUCT} that is used by default.
     *
     * @param recoveryStrategy specifies the reaction which should be applied in case of previously saved state of bucket has been lost.
     *
     * @return {@code this}
     */
    RemoteAsyncBucketBuilder<K> withRecoveryStrategy(RecoveryStrategy recoveryStrategy);

    /**
     * Configures the optimization strategy that will be applied for buckets that are built by this builder.
     *
     * <p>
     * It is worth mentioning that optimization will take effect only if you reuse the bucket, so you need to store a reference to the bucket anywhere in order to reuse it later. In other words, if any request optimization strategy has been applied to the bucket proxy then proxy can not be treated as a cheap object.
     *
     * <p>
     *   * The full list of built-in optimizations can be found there {@link io.github.bucket4j.distributed.proxy.optimization.Optimizations}
     *
     * @param optimization optimization strategy
     *
     * @return {@code this}
     */
    RemoteAsyncBucketBuilder<K> withOptimization(Optimization optimization);

    /**
     * Builds the {@link AsyncBucketProxy}. Proxy is being created in lazy mode, its state is not persisted in external storage until first interaction,
     * so if you want to save bucket state immediately then just call {@link AsyncBucketProxy#getAvailableTokens()}.
     *
     * <p>
     *     If you had not used {@link #withOptimization(Optimization)} during construction then created proxy can be treated as cheap object,
     *     feel free just build, use and forget as many proxies under the same key as you need, do not cache the built instances.
     * </p>
     *
     * @param key the key that used in external storage to distinguish one bucket from another.
     * @param configuration limits configuration
     *
     * @return new instance of {@link AsyncBucketProxy} created in lazy mode.
     */
    AsyncBucketProxy build(K key, BucketConfiguration configuration);

    /**
     * Has the same semantic with {@link #build(Object, BucketConfiguration)},
     * but additionally provides ability to provide configuration lazily, that can be helpful when figuring-out the right configuration parameters
     * is costly, for example because parameters for particular {@code key} are stored in external database,
     * {@code configurationSupplier} will be called if and only if bucket has not been persisted before.
     *
     *
     * @param key the key that used in external storage to distinguish one bucket from another.
     * @param configurationSupplier provider for bucket configuration
     *
     * @return new instance of {@link AsyncBucketProxy} created in lazy mode.
     */
    AsyncBucketProxy build(K key, Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier);

}
