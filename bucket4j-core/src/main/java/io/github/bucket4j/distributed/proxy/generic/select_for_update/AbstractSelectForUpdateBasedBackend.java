/*
 *
 * Copyright 2015-2019 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.distributed.proxy.generic.select_for_update;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.proxy.generic.GenericEntry;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractSelectForUpdateBasedBackend<K extends Serializable> implements Backend<K> {

    private final TimeMeter timeMeter;

    protected AbstractSelectForUpdateBasedBackend(TimeMeter timeMeter) {
        this.timeMeter = timeMeter;
    }

    protected AbstractSelectForUpdateBasedBackend() {
        timeMeter = TimeMeter.SYSTEM_MILLISECONDS;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        SelectForUpdateBasedTransaction transaction = allocateTransaction(key);
        try {
            return execute(command, transaction);
        } finally {
            releaseTransaction(transaction);
        }
    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        throw new UnsupportedOperationException();
    }

    protected abstract SelectForUpdateBasedTransaction allocateTransaction(K key);

    protected abstract void releaseTransaction(SelectForUpdateBasedTransaction transaction);

    private <T extends Serializable> CommandResult<T> execute(RemoteCommand<T> command, SelectForUpdateBasedTransaction transaction) {
        transaction.begin();
        try {
            byte[] stateBytes = transaction.lockAndGet().orElse(null);
            GenericEntry entry = new GenericEntry(stateBytes);
            CommandResult<T> result = command.execute(entry, timeMeter.currentTimeNanos());
            if (entry.isModified()) {
                byte[] bytes = entry.getModifiedStateBytes();
                transaction.update(bytes);
            }
            transaction.commit();
            return result;
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }
    }

}
