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
     * Configures custom recovery strategy instead of {@link RecoveryStrategy#RECONSTRUCT} that is used by default.
     *
     * @param recoveryStrategy specifies the reaction which should be applied in case of previously saved state of bucket has been lost, explicitly removed or expired.
     *
     * @return {@code this}
     */
    RemoteBucketBuilder<K> withRecoveryStrategy(RecoveryStrategy recoveryStrategy);

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
     * Has the same semantic with {@link #build(Object, BucketConfiguration)},
     * but additionally provides ability to provide configuration lazily, that can be helpful when figuring-out the right configuration parameters
     * is costly, for example because parameters for particular {@code key} are stored in external database,
     * {@code configurationSupplier} will be called if and only if bucket has not been persisted before.
     *
     * @param key the key that used in external storage to distinguish one bucket from another.
     * @param configurationSupplier provider for bucket configuration
     *
     * @return new instance of {@link BucketProxy} created in lazy mode.
     */
    BucketProxy build(K key, Supplier<BucketConfiguration> configurationSupplier);

    /**
     * Builds the {@link BucketProxy}. Proxy is being created in lazy mode, its state is not persisted in external storage until first interaction,
     * so if you want to save bucket state immediately then just call {@link BucketProxy#getAvailableTokens()}.
     *
     * <p>
     *     If you had not used {@link #withOptimization(Optimization)} during construction then created proxy can be treated as cheap object,
     *     feel free just build, use and forget as many proxies under the same key as you need, do not cache the built instances.
     * </p>
     *
     * @param key the key that used in external storage to distinguish one bucket from another.
     * @param configuration limits configuration
     *
     * @return new instance of {@link BucketProxy} created in lazy mode.
     * @deprecated use {@link #build(Object, Supplier)} instead. This method will be removed soon
     */
    @Deprecated
    BucketProxy build(K key, BucketConfiguration configuration);


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
        return new RemoteBucketBuilder<>() {
            @Override
            public RemoteBucketBuilder<K1> withRecoveryStrategy(RecoveryStrategy recoveryStrategy) {
                RemoteBucketBuilder.this.withRecoveryStrategy(recoveryStrategy);
                return this;
            }

            @Override
            public RemoteBucketBuilder<K1> withOptimization(Optimization optimization) {
                RemoteBucketBuilder.this.withOptimization(optimization);
                return this;
            }

            @Override
            public RemoteBucketBuilder<K1> withImplicitConfigurationReplacement(long desiredConfigurationVersion, TokensInheritanceStrategy tokensInheritanceStrategy) {
                RemoteBucketBuilder.this.withImplicitConfigurationReplacement(desiredConfigurationVersion, tokensInheritanceStrategy);
                return this;
            }

            @Override
            public BucketProxy build(K1 key, Supplier<BucketConfiguration> configurationSupplier) {
                return RemoteBucketBuilder.this.build(mapper.apply(key), configurationSupplier);
            }

            @Override
            public BucketProxy build(K1 key, BucketConfiguration configuration) {
                return RemoteBucketBuilder.this.build(mapper.apply(key), configuration);
            }

            // To prevent nesting of anonymous class instances, directly map the original instance.
            @Override
            public <K2> RemoteBucketBuilder<K2> withMapper(Function<? super K2, ? extends K1> innerMapper) {
                return RemoteBucketBuilder.this.withMapper(mapper.compose(innerMapper));
            }
        };
    }
}
