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

import java.util.Optional;

import io.github.bucket4j.distributed.remote.RemoteBucketState;

public interface CompareAndSwapOperation {

    /**
     * Reads data if it exists
     *
     * @param timeoutNanos optional timeout in nanoseconds
     *
     * @return persisted data or empty optional if data not exists
     */
    Optional<byte[]> getStateData(Optional<Long> timeoutNanos);

    /**
     * Compares and swap data associated with key
     *
     * @param originalData previous bucket state(can be null).
     * @param newData new bucket state
     * @param newState new state of bucket - can be used to extract additional data is useful for persistence or logging.
     * @param timeoutNanos optional timeout in nanoseconds
     *
     * @return {@code true} if data changed, {@code false} if another parallel transaction achieved success instead of current transaction
     */
    boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos);

}
