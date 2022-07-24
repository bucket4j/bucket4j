/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2022 Vladimir Bukhtoyarov
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
package io.github.bucket4j.redis;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;

import java.util.Objects;

public class AbstractRedisProxyManagerBuilder<T extends AbstractRedisProxyManagerBuilder> {

    private ExpirationAfterWriteStrategy expirationStrategy;
    private ClientSideConfig clientSideConfig = ClientSideConfig.getDefault();

    public T withExpirationStrategy(ExpirationAfterWriteStrategy expirationStrategy) {
        this.expirationStrategy = Objects.requireNonNull(expirationStrategy);
        return (T) this;
    }

    public T withClientSideConfig(ClientSideConfig clientSideConfig) {
        this.clientSideConfig = Objects.requireNonNull(clientSideConfig);
        return (T) this;
    }

    public ExpirationAfterWriteStrategy getNotNullExpirationStrategy() {
        if (expirationStrategy == null) {
            throw new IllegalStateException("expirationStrategy is not configured");
        }
        return expirationStrategy;
    }

    public ClientSideConfig getClientSideConfig() {
        return clientSideConfig;
    }

}
