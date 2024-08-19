/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2021 Vladimir Bukhtoyarov
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
package io.github.bucket4j.distributed;

import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronization;

import java.time.Duration;

/**
 * The synchronization controller for {@link BucketProxy}.
 *
 * <p>
 * This interface is actual only if a synchronization was applied during bucket construction via {@link RemoteBucketBuilder#withSynchronization(BucketSynchronization)}
 * otherwise all methods of controller will do nothing.
 */
public interface BucketSynchronizationController {

    /**
     * Initiates immediate synchronization of local copy of bucket with remote storage
     */
    default void syncImmediately() {
        syncByCondition(0L, Duration.ZERO);
    }

    /**
     * Initiates immediate synchronization of local copy of bucket with remote storage in case of both conditions bellow are {@code true}:
     * <ul>
     *     <li>Accumulated amount of locally consumed tokens without external synchronization is greater than or equal to {@code unsynchronizedTokens}</li>
     *     <li>Time passed since last synchronization with external storage is greater than or equal to {@code timeSinceLastSync}</li>
     * </ul>
     *
     * @param unsynchronizedTokens criterion for accumulated amount of unsynchronized tokens
     * @param timeSinceLastSync criterion for time passed since last synchronization
     */
    void syncByCondition(long unsynchronizedTokens, Duration timeSinceLastSync);

}
