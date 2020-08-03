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

package io.github.bucket4j.distributed.proxy.generic.compare_and_swap;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AbstractBackend;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.GenericEntry;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.versioning.Version;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractCompareAndSwapBasedBackend<K> extends AbstractBackend<K> {

    protected AbstractCompareAndSwapBasedBackend(ClientSideConfig clientSideConfig) {
        super(injectTimeClock(clientSideConfig));
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        while (true) {
            CompareAndSwapBasedTransaction transaction = allocateTransaction(key);
            try {
                CommandResult<T> result = execute(request, transaction);
                if (result != null) {
                    return result;
                }
            } finally {
                releaseTransaction(transaction);
            }
        }
    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        throw new UnsupportedOperationException();
    }

    protected abstract CompareAndSwapBasedTransaction allocateTransaction(K key);

    protected abstract void releaseTransaction(CompareAndSwapBasedTransaction transaction);

    private <T> CommandResult<T> execute(Request<T> request, CompareAndSwapBasedTransaction transaction) {
        RemoteCommand<T> command = request.getCommand();
        byte[] originalStateBytes = transaction.get().orElse(null);
        GenericEntry entry = new GenericEntry(originalStateBytes, request.getBackwardCompatibilityVersion());
        CommandResult<T> result = command.execute(entry, getClientSideTime());
        if (!entry.isModified()) {
            return result;
        }

        byte[] newStateBytes = entry.getModifiedStateBytes();
        if (transaction.compareAndSwap(originalStateBytes, newStateBytes)) {
            return result;
        } else {
            return null;
        }
    }

    private static ClientSideConfig injectTimeClock(ClientSideConfig clientSideConfig) {
        if (clientSideConfig.getClientSideClock().isPresent()) {
            return clientSideConfig;
        }
        return ClientSideConfig.withClientClockAndCompatibility(TimeMeter.SYSTEM_MILLISECONDS, clientSideConfig.getBackwardCompatibilityVersion());
    }

}
