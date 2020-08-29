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
package io.github.bucket4j.distributed;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketListener;

import java.time.Duration;

public interface BucketProxy extends Bucket {

    @Override
    BucketProxy toListenable(BucketListener listener);

    /**
     * TODO javadocs
     */
    default void syncImmediately() {
        syncByCondition(0L, Duration.ZERO);
    }

    /**
     *
     * @param unsynchronizedTokens
     * @param timeSinceLastSync
     */
    void syncByCondition(long unsynchronizedTokens, Duration timeSinceLastSync);

}
