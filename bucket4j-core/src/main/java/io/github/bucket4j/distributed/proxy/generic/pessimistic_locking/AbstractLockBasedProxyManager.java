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

package io.github.bucket4j.distributed.proxy.generic.pessimistic_locking;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.GenericEntry;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.LockAndGetResult;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.Request;

import java.util.concurrent.CompletableFuture;

/**
 * The base class for proxy managers that built on top of idea that underlining storage provide Compare-And-Swap functionality.
 *
 * @param <K> the generic type for unique identifiers that used to point to the bucket in external storage.
 */
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
            transaction.release();
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

    @Override
    protected CompletableFuture<?> removeAsync(Object key) {
        return null;
    }

    protected abstract LockBasedTransaction allocateTransaction(K key);

    private <T> CommandResult<T> execute(Request<T> request, LockBasedTransaction transaction) {
        RemoteCommand<T> command = request.getCommand();
        transaction.begin();

        // lock and get data
        byte[] persistedDataOnBeginOfTransaction;
        try {
            persistedDataOnBeginOfTransaction = transaction.lockAndGet();
        } catch (Throwable t) {
            unlockAndRollback(transaction);
            throw new BucketExceptions.BucketExecutionException(t);
        }

        // check that command is able to provide initial state in case of bucket does not exist
        if (persistedDataOnBeginOfTransaction == null && !request.getCommand().isInitializationCommand()) {
            unlockAndRollback(transaction);
            return CommandResult.bucketNotFound();
        }

        try {
            GenericEntry entry = new GenericEntry(persistedDataOnBeginOfTransaction, request.getBackwardCompatibilityVersion());
            CommandResult<T> result = command.execute(entry, super.getClientSideTime());
            if (entry.isModified()) {
                byte[] bytes = entry.getModifiedStateBytes();
                if (persistedDataOnBeginOfTransaction == null) {
                    transaction.create(bytes, entry.getModifiedState());
                } else {
                    transaction.update(bytes, entry.getModifiedState());
                }
            }
            transaction.unlock();
            transaction.commit();
            return result;
        } catch (Throwable t) {
            unlockAndRollback(transaction);
            throw new BucketExceptions.BucketExecutionException(t);
        }
    }

    private void unlockAndRollback(LockBasedTransaction transaction) {
        try {
            transaction.unlock();
        } finally {
            transaction.rollback();
        }
    }

    private static ClientSideConfig injectTimeClock(ClientSideConfig clientSideConfig) {
        if (clientSideConfig.getClientSideClock().isPresent()) {
            return clientSideConfig;
        }
        return clientSideConfig.withClientClock(TimeMeter.SYSTEM_MILLISECONDS);
    }

}
