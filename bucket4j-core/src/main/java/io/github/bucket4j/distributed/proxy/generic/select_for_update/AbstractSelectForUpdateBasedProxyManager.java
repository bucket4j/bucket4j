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

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.proxy.ProxyManagerConfig;
import io.github.bucket4j.distributed.proxy.Timeout;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.Request;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


/**
 * The base class for proxy managers that built on top of idea that underlining storage provide Compare-And-Swap functionality.
 *
 * @param <K> the generic type for unique identifiers that used to point to the bucket in external storage.
 */
public abstract class AbstractSelectForUpdateBasedProxyManager<K> extends AbstractProxyManager<K> {

    private static final CommandResult RETRY_IN_THE_SCOPE_OF_NEW_TRANSACTION = CommandResult.success(true, 666);

    protected AbstractSelectForUpdateBasedProxyManager(ProxyManagerConfig proxyManagerConfig) {
        super(injectTimeClock(proxyManagerConfig));
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        Timeout timeout = Timeout.of(getClientSideConfig());
        while (true) {
            SelectForUpdateBasedTransaction transaction = timeout.call(timeoutNanos -> allocateTransaction(key, timeoutNanos));
            CommandResult<T> result;
            try {
                result = execute(request, transaction, timeout);
            } finally {
                transaction.release();
            }
            if (result != RETRY_IN_THE_SCOPE_OF_NEW_TRANSACTION) {
                return result;
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

    @Override
    protected CompletableFuture<Void> removeAsync(Object key) {
        return null;
    }

    protected abstract SelectForUpdateBasedTransaction allocateTransaction(K key, Optional<Long> timeoutNanos);

    private <T> CommandResult<T> execute(Request<T> request, SelectForUpdateBasedTransaction transaction, Timeout timeout) {
        RemoteCommand<T> command = request.getCommand();
        timeout.run(transaction::begin);

        // lock and get data
        LockAndGetResult lockResult;
        byte[] persistedDataOnBeginOfTransaction;
        try {
            lockResult = timeout.call(transaction::tryLockAndGet);
        } catch (Throwable t) {
            transaction.rollback();
            throw BucketExceptions.from(t);
        }

        // insert data that can be locked in next transaction if data does not exist
        if (!lockResult.isLocked()) {
            try {
                if (timeout.call(transaction::tryInsertEmptyData)) {
                    timeout.run(transaction::commit);
                } else {
                    transaction.rollback();
                }
                return RETRY_IN_THE_SCOPE_OF_NEW_TRANSACTION;
            } catch (Throwable t) {
                transaction.rollback();
                throw BucketExceptions.from(t);
            }
        }

        // check that command is able to provide initial state in case of bucket does not exist
        persistedDataOnBeginOfTransaction = lockResult.getData();
        if (persistedDataOnBeginOfTransaction == null && !request.getCommand().isInitializationCommand()) {
            transaction.rollback();
            return CommandResult.bucketNotFound();
        }

        try {
            MutableBucketEntry entry = new MutableBucketEntry(persistedDataOnBeginOfTransaction);
            CommandResult<T> result = command.execute(entry, super.getClientSideTime());
            if (entry.isStateModified()) {
                RemoteBucketState modifiedState = entry.get();
                byte[] bytes = entry.getStateBytes(request.getBackwardCompatibilityVersion());
                timeout.run(threshold -> transaction.update(bytes, modifiedState, threshold));
            }
            timeout.run(transaction::commit);
            return result;
        } catch (Throwable t) {
            transaction.rollback();
            throw BucketExceptions.from(t);
        }
    }

    private static ProxyManagerConfig injectTimeClock(ProxyManagerConfig proxyManagerConfig) {
        if (proxyManagerConfig.getClientSideClock().isPresent()) {
            return proxyManagerConfig;
        }
        return proxyManagerConfig.withClientClock(TimeMeter.SYSTEM_MILLISECONDS);
    }

    protected void applyTimeout(PreparedStatement statement, Optional<Long> requestTimeoutNanos) throws SQLException {
        if (requestTimeoutNanos.isPresent()) {
            int timeoutSeconds = (int) Math.max(1, TimeUnit.NANOSECONDS.toSeconds(requestTimeoutNanos.get()));
            statement.setQueryTimeout(timeoutSeconds);
        }
    }

}
