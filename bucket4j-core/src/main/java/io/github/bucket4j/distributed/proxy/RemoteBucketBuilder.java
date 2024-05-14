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
import io.github.bucket4j.BucketListener;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.optimization.Optimization;


import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The builder for {@link BucketProxy}
 *
 * @param <K> the type of keys in the external storage
 *
 * @see ProxyManager
 */
public interface RemoteBucketBuilder<K> {

    /**
     * Configures the optimization strategy that will be applied for buckets that are built by this builder.
     *
     * <p>
     * It is worth mentioning that optimization will take effect only if you reuse the bucket, so you need to store a reference to the bucket anywhere in order to reuse it later.
     * In other words, if any request optimization strategy has been applied to the bucket proxy then proxy can not be treated as a cheap object.
     *
     * <p>
 *   * The full list of built-in optimizations can be found there {@link io.github.bucket4j.distributed.proxy.optimization.Optimizations}
     *
     * @param optimization optimization strategy
     *
     * @return {@code this}
     */
    RemoteBucketBuilder<K> withOptimization(Optimization optimization);

    /**
     * Activates implicit configuration replacement.
     *
     * <p> By default distributed bucket operates with configuration that was provided at the time of its first creation.
     * Providing the new configuration via {@link RemoteBucketBuilder} takes no effect if bucket is already persisted in the storage, because configuration is stored together with state of bucket.
     * Without implicit configuration replacement, there is only one way to replace configuration of bucket - is explicit calling of {@link io.github.bucket4j.Bucket#replaceConfiguration(BucketConfiguration, TokensInheritanceStrategy)}.
     *
     * <p>
     * When implicit configuration replacement is activated,
     * bucket will automatically replace persisted configuration using provided {@code tokensInheritanceStrategy}
     * in case of persisted version of configuration in the storage < than provided {@code desiredConfigurationVersion}.
     *
     * @param desiredConfigurationVersion specifies desired configuration version
     * @param tokensInheritanceStrategy the strategy that will be used for token migration if {@code desiredConfigurationVersion of persisted bucket} is less that provided desiredConfigurationVersion
     *
     * @return {@code this}
     */
    RemoteBucketBuilder<K> withImplicitConfigurationReplacement(long desiredConfigurationVersion, TokensInheritanceStrategy tokensInheritanceStrategy);

    /**
     * Specifies {@code listener} for buckets that will be created by this builder.
     *
     * @param listener the listener of bucket events.
     *
     * @return this builder instance
     */
    RemoteBucketBuilder<K> withListener(BucketListener listener);

    /**
     * Builds the {@link BucketProxy}. Proxy is being created in lazy mode, its state is not persisted in external storage until first interaction.
     * {@code configurationSupplier} will be called if and only if bucket has not been persisted before.
     *
     * @param key the key that used in external storage to distinguish one bucket from another.
     * @param configurationSupplier provider for bucket configuration
     *
     * @return new instance of {@link BucketProxy}
     */
    BucketProxy build(K key, Supplier<BucketConfiguration> configurationSupplier);


    /**
     * Returns a proxy object that wraps this RemoteBucketBuilder such that keys are first mapped using the specified mapping function
     * before being sent to the remote store. The returned RemoteBucketBuilder shares the same underlying store as the original,
     * and keys that map to the same value will share the same remote state.
     *
     * @param mapper the mapper function to apply to keys
     * @return a proxy object that wraps this RemoteBucketBuilder
     * @param <K1> the type of key accepted by returned RemoteBucketBuilder
     */
    default <K1> RemoteBucketBuilder<K1> withMapper(Function<? super K1, ? extends K> mapper) {
        return new RemoteBucketBuilderView(this, mapper);
    }

}
