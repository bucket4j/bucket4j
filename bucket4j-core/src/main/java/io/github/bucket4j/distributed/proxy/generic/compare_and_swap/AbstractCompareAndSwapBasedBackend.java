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
import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.proxy.generic.GenericEntry;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractCompareAndSwapBasedBackend<K extends Serializable> implements Backend<K> {

    private final TimeMeter timeMeter;

    protected AbstractCompareAndSwapBasedBackend(TimeMeter timeMeter) {
        this.timeMeter = timeMeter;
    }

    protected AbstractCompareAndSwapBasedBackend() {
        timeMeter = TimeMeter.SYSTEM_MILLISECONDS;
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        while (true) {
            CompareAndSwapBasedTransaction transaction = allocateTransaction(key);
            try {
                CommandResult<T> result = execute(command, transaction);
                if (result != null) {
                    return result;
                }
            } finally {
                releaseTransaction(transaction);
            }
        }
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        throw new UnsupportedOperationException();
    }

    protected abstract CompareAndSwapBasedTransaction allocateTransaction(K key);

    protected abstract void releaseTransaction(CompareAndSwapBasedTransaction transaction);

    private <T extends Serializable> CommandResult<T> execute(RemoteCommand<T> command, CompareAndSwapBasedTransaction transaction) {
        byte[] originalStateBytes = transaction.get().orElse(null);
        GenericEntry entry = new GenericEntry(originalStateBytes);
        CommandResult<T> result = command.execute(entry, timeMeter.currentTimeNanos());
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

}
