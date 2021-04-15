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
import io.github.bucket4j.distributed.proxy.AbstractBackend;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.GenericEntry;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.Request;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractCompareAndSwapBasedBackend<K> extends AbstractBackend<K> {

    private static final CommandResult<?> UNSUCCESSFUL_CAS_RESULT = null;

    protected AbstractCompareAndSwapBasedBackend(ClientSideConfig clientSideConfig) {
        super(injectTimeClock(clientSideConfig));
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        CompareAndSwapOperation operation = beginCompareAndSwapOperation(key);
        while (true) {
            CommandResult<T> result = execute(request, operation);
            if (result != UNSUCCESSFUL_CAS_RESULT) {
                return result;
            }
        }
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        AsyncCompareAndSwapOperation operation = beginAsyncCompareAndSwapOperation(key);
        CompletableFuture<CommandResult<T>> result = executeAsync(request, operation);
        return result.thenCompose((CommandResult<T> response) -> retryIfCasWasUnsuccessful(operation, request, response));
    }

    /**
     * TODO
     * @param key
     * @return
     */
    protected abstract CompareAndSwapOperation beginCompareAndSwapOperation(K key);

    /**
     * TODO
     * @param key
     * @return
     */
    protected abstract AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key);

    private <T> CommandResult<T> execute(Request<T> request, CompareAndSwapOperation operation) {
        RemoteCommand<T> command = request.getCommand();
        byte[] originalStateBytes = operation.getStateData().orElse(null);
        GenericEntry entry = new GenericEntry(originalStateBytes, request.getBackwardCompatibilityVersion());
        CommandResult<T> result = command.execute(entry, getClientSideTime());
        if (!entry.isModified()) {
            return result;
        }

        byte[] newStateBytes = entry.getModifiedStateBytes();
        if (operation.compareAndSwap(originalStateBytes, newStateBytes)) {
            return result;
        } else {
            return null;
        }
    }

    private <T> CompletableFuture<CommandResult<T>> retryIfCasWasUnsuccessful(AsyncCompareAndSwapOperation operation, Request<T> request, CommandResult<T> casResponse) {
        if (casResponse != UNSUCCESSFUL_CAS_RESULT) {
            return CompletableFuture.completedFuture(casResponse);
        } else {
            return executeAsync(request, operation).thenCompose((CommandResult<T> response) -> retryIfCasWasUnsuccessful(operation, request, response));
        }
    }


    private <T> CompletableFuture<CommandResult<T>> executeAsync(Request<T> request, AsyncCompareAndSwapOperation operation) {
        return operation.getStateData()
            .thenApply((Optional<byte[]> originalStateBytes) -> originalStateBytes.orElse(null))
            .thenCompose((byte[] originalStateBytes) -> {
                RemoteCommand<T> command = request.getCommand();
                GenericEntry entry = new GenericEntry(originalStateBytes, request.getBackwardCompatibilityVersion());
                CommandResult<T> result = command.execute(entry, getClientSideTime());
                if (!entry.isModified()) {
                    return CompletableFuture.completedFuture(result);
                }

                byte[] newStateBytes = entry.getModifiedStateBytes();
                return operation.compareAndSwap(originalStateBytes, newStateBytes).thenApply((casWasSuccessful) -> casWasSuccessful? result : null);
            });
    }

    private static ClientSideConfig injectTimeClock(ClientSideConfig clientSideConfig) {
        if (clientSideConfig.getClientSideClock().isPresent()) {
            return clientSideConfig;
        }
        return ClientSideConfig.withClientClockAndCompatibility(TimeMeter.SYSTEM_MILLISECONDS, clientSideConfig.getBackwardCompatibilityVersion());
    }

}
