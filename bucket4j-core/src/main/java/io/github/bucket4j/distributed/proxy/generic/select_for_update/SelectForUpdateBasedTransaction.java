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

/**
 * Describes the set of operations that {@link AbstractSelectForUpdateBasedProxyManager} typically performs in reaction to user request.
 * The typical flow is following:
 * <ol>
 *     <li>begin - {@link #begin()}</li>
 *     <li>lock - {@link #tryLockAndGet()}</li>
 *     <li>update - {@link #update(byte[])}</li>
 *     <li>commit - {@link #commit()}</li>
 *     <li>release - {@link #release()}</li>
 * </ol>
 */
public interface SelectForUpdateBasedTransaction {

    /**
     * Begins transaction if underlying storage requires transactions.
     * There is strong guarantee that {@link #commit()} or {@link #rollback()} will be called if {@link #begin()} returns successfully.
     */
    void begin();

    /**
     * Rollbacks transaction if underlying storage requires transactions
     */
    void rollback();

    /**
     * Commits transaction if underlying storage requires transactions
     */
    void commit();

    /**
     * Locks data by the key associated with this transaction and returns data that is associated with the key.
     *
     * @return the data by the key associated with this transaction, or null data associated with key does not exist
     */
    LockAndGetResult tryLockAndGet();

    /**
     * Creates empty data by for the key associated with this transaction.
     * This operation is required to be able to lock data in the scope of next transaction.
     *
     * @return true if data has been inserted
     */
    boolean tryInsertEmptyData();

    /**
     * Updates the data by the key associated with this transaction.
     *
     * @param data bucket state to persists
     */
    void update(byte[] data);

    /**
     * Frees resources associated with this transaction
     */
    void release();
}
