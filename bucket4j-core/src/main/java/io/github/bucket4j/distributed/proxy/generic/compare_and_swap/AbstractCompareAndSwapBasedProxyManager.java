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

package io.github.bucket4j.distributed.proxy.generic.compare_and_swap;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.proxy.ProxyManagerConfig;
import io.github.bucket4j.distributed.proxy.Timeout;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.Request;

/**
 * The base class for proxy managers that built on top of idea that underlining storage provide transactions and locking.
 *
 * @param <K> the generic type for unique identifiers that used to point to the bucket in external storage.
 */
public abstract class AbstractCompareAndSwapBasedProxyManager<K> extends AbstractProxyManager<K> {

    private static final CommandResult<?> UNSUCCESSFUL_CAS_RESULT = null;

    protected AbstractCompareAndSwapBasedProxyManager(ProxyManagerConfig proxyManagerConfig) {
        super(injectTimeClock(proxyManagerConfig));
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        Timeout timeout = Timeout.of(getConfig());
        CompareAndSwapOperation operation = timeout.call(requestTimeout -> beginCompareAndSwapOperation(key));
        while (true) {
            CommandResult<T> result = execute(request, operation, timeout);
            if (result != UNSUCCESSFUL_CAS_RESULT) {
                return result;
            }
        }
    }

    protected abstract CompareAndSwapOperation beginCompareAndSwapOperation(K key);

    private <T> CommandResult<T> execute(Request<T> request, CompareAndSwapOperation operation, Timeout timeout) {
        RemoteCommand<T> command = request.getCommand();
        byte[] originalStateBytes = timeout.call(operation::getStateData).orElse(null);
        MutableBucketEntry entry = new MutableBucketEntry(originalStateBytes);
        CommandResult<T> result = command.execute(entry, getClientSideTime());
        if (!entry.isStateModified()) {
            return result;
        }

        byte[] newStateBytes = entry.getStateBytes(request.getBackwardCompatibilityVersion());
        if (timeout.call(requestTimeout -> operation.compareAndSwap(originalStateBytes, newStateBytes, entry.get(), requestTimeout))) {
            return result;
        } else {
            return null;
        }
    }

    private static ProxyManagerConfig injectTimeClock(ProxyManagerConfig proxyManagerConfig) {
        if (proxyManagerConfig.getClientSideClock().isPresent()) {
            return proxyManagerConfig;
        }
        return proxyManagerConfig.withClientClock(TimeMeter.SYSTEM_MILLISECONDS);
    }

}
