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

import java.util.Optional;

import io.github.bucket4j.distributed.remote.RemoteBucketState;

/**
 * Describes the set of operations that {@link AbstractSelectForUpdateBasedProxyManager} typically performs in reaction to user request.
 * The typical flow is following:
 * <ol>
 *     <li>begin - {@link #begin(Optional)}</li>
 *     <li>lock - {@link #tryLockAndGet(Optional)}</li>
 *     <li>update - {@link #update(byte[],RemoteBucketState,Optional)}</li>
 *     <li>commit - {@link #commit(Optional)}</li>
 *     <li>release - {@link #release()}</li>
 * </ol>
 */
public interface SelectForUpdateBasedTransaction {

    /**
     * Begins transaction if underlying storage requires transactions.
     * There is strong guarantee that {@link #commit(Optional)} or {@link #rollback(Optional)} will be called if {@link #begin(Optional)} returns successfully.
     *
     * @param timeoutNanos optional timeout in nanoseconds
     */
    void begin(Optional<Long> timeoutNanos);

    /**
     * Rollbacks transaction if underlying storage requires transactions
     *
     * @param timeoutNanos optional timeout in nanoseconds
     */
    void rollback(Optional<Long> timeoutNanos);

    /**
     * Commits transaction if underlying storage requires transactions
     *
     * @param timeoutNanos optional timeout in nanoseconds
     */
    void commit(Optional<Long> timeoutNanos);

    /**
     * Locks data by the key associated with this transaction and returns data that is associated with the key.
     *
     * @param timeoutNanos optional timeout in nanoseconds
     *
     * @return the data by the key associated with this transaction, or null data associated with key does not exist
     */
    LockAndGetResult tryLockAndGet(Optional<Long> timeoutNanos);

    /**
     * Creates empty data by for the key associated with this transaction.
     * This operation is required to be able to lock data in the scope of next transaction.
     *
     * @param timeoutNanos optional timeout in nanoseconds
     *
     * @return true if data has been inserted
     */
    boolean tryInsertEmptyData(Optional<Long> timeoutNanos);

    /**
     * Updates the data by the key associated with this transaction.
     *
     * @param data bucket state to persists
     * @param newState new state of bucket - can be used to extract additional data is useful for persistence or logging.
     * @param timeoutNanos optional timeout in nanoseconds
     */
    void update(byte[] data, RemoteBucketState newState, Optional<Long> timeoutNanos);

    /**
     * Frees resources associated with this transaction
     */
    void release();
}
