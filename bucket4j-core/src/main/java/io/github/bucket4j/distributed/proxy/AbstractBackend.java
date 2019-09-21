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

package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.AsyncBucket;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.commands.GetConfigurationCommand;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class AbstractBackend<K extends Serializable> implements Backend<K> {

    @Override
    public BucketProxy<K> getDurableProxy(K key, Supplier<BucketConfiguration> configurationSupplier, RecoveryStrategy recoveryStrategy) {
        return BucketProxy.createInitializedBucket(key, configurationSupplier, this, recoveryStrategy);
    }

    @Override
    public Optional<BucketConfiguration> getProxyConfiguration(K key) {
        GetConfigurationCommand cmd = new GetConfigurationCommand();
        CommandResult<BucketConfiguration> result = this.execute(key, cmd);
        if (result.isBucketNotFound()) {
            return Optional.empty();
        }
        return Optional.of(result.getData());
    }

}
