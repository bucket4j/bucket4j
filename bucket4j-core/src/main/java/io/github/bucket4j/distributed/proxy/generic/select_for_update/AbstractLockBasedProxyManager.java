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

package io.github.bucket4j.distributed.proxy.generic.select_for_update;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.GenericEntry;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.Request;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractLockBasedProxyManager<K> extends AbstractProxyManager<K> {

    protected AbstractLockBasedProxyManager(ClientSideConfig clientSideConfig) {
        super(injectTimeClock(clientSideConfig));
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        LockBasedTransaction transaction = allocateTransaction(key);
        try {
            return execute(request, transaction);
        } finally {
            releaseTransaction(transaction);
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

    protected abstract LockBasedTransaction allocateTransaction(K key);

    protected abstract void releaseTransaction(LockBasedTransaction transaction);

    private <T> CommandResult<T> execute(Request<T> request, LockBasedTransaction transaction) {
        RemoteCommand<T> command = request.getCommand();
        transaction.begin();
        try {
            try {
                byte[] persistedDataOnBeginOfTransaction;
                LockResult lockResult = transaction.lock();
                if (lockResult == LockResult.DATA_EXISTS_AND_LOCKED) {
                    persistedDataOnBeginOfTransaction = transaction.getData();
                } else if (command.isInitializationCommand()) {
                    persistedDataOnBeginOfTransaction = null;
                } else {
                    return CommandResult.bucketNotFound();
                }
                GenericEntry entry = new GenericEntry(persistedDataOnBeginOfTransaction, request.getBackwardCompatibilityVersion());
                CommandResult<T> result = command.execute(entry, super.getClientSideTime());
                if (entry.isModified()) {
                    byte[] bytes = entry.getModifiedStateBytes();
                    if (persistedDataOnBeginOfTransaction == null) {
                        transaction.create(bytes);
                    } else {
                        transaction.update(bytes);
                    }
                }
                transaction.commit();
                return result;
            } finally {
                transaction.unlock();
            }
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }
    }

    private static ClientSideConfig injectTimeClock(ClientSideConfig clientSideConfig) {
        if (clientSideConfig.getClientSideClock().isPresent()) {
            return clientSideConfig;
        }
        return ClientSideConfig.withClientClockAndCompatibility(TimeMeter.SYSTEM_MILLISECONDS, clientSideConfig.getBackwardCompatibilityVersion());
    }

}
