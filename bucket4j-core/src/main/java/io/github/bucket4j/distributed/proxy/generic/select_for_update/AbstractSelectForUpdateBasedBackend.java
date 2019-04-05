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

import io.github.bucket4j.MathType;
import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.proxy.BackendOptions;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractSelectForUpdateBasedBackend<K extends Serializable> implements Backend<K> {

    private static final BackendOptions OPTIONS = new BackendOptions(false, MathType.ALL, MathType.INTEGER_64_BITS);

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        SelectForUpdateBasedTransaction<K> transaction = createTransaction();
        try {
            Optional<byte[]> state = transaction.selectForUpdate(key);
        } finally {

        }
        return null;
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BackendOptions getOptions() {
        return OPTIONS;
    }

    protected abstract SelectForUpdateBasedTransaction<K> createTransaction();


    private static class Entry implements MutableBucketEntry {

        private final Optional<byte[]> originalStateBytes;

        private RemoteBucketState parsedState;
        private RemoteBucketState modifiedState;

        private Entry(Optional<byte[]> originalStateBytes) {
            this.originalStateBytes = originalStateBytes;
        }

        @Override
        public boolean exists() {
            return originalStateBytes.isPresent();
        }

        @Override
        public void set(RemoteBucketState state) {
            modifiedState = state;
        }

        @Override
        public RemoteBucketState get() {
            if (parsedState != null) {

            }
            return null;
        }

    }

}
