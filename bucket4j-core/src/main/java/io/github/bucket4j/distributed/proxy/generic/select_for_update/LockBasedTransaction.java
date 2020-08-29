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
 * TODO
 */
public interface LockBasedTransaction {

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
     * Locks data by the key associated with this transaction.
     * There is strong guarantee that {@link #unlock()} will be called if {@link #lock()} returns successfully.
     *
     * @return
     */
    LockResult lock();

    /**
     * Unlocks data by the key associated with this transaction.
     *
     * @return {@code true} if data has been locked, {@code false} if data does not exists
     */
    void unlock();

    /**
     * Returns the data by the key associated with this transaction.
     *
     * @return persisted state of bucket
     */
    byte[] getData();

    /**
     * Creates the data by the key associated with this transaction.
     *
     * @param data bucket state to persists
     */
    void create(byte[] data);

    /**
     * Updates the data by the key associated with this transaction.
     *
     * @param data bucket state to persists
     */
    void update(byte[] data);

}
