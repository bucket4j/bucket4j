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
package io.github.bucket4j;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Verbose API for {@link SchedulingBucket}
 */
public interface VerboseSchedulingBucket {

    /**
     * Has the same semantic as {@link SchedulingBucket#tryConsume(long, long, ScheduledExecutorService)}
     */
    CompletableFuture<VerboseResult<Boolean>> tryConsume(long numTokens, long maxWaitNanos, ScheduledExecutorService scheduler);

    /**
     * Has the same semantic as {@link SchedulingBucket#tryConsume(long, Duration, ScheduledExecutorService)}
     */
    default CompletableFuture<VerboseResult<Boolean>> tryConsume(long numTokens, Duration maxWait, ScheduledExecutorService scheduler) {
        return tryConsume(numTokens, maxWait.toNanos(), scheduler);
    }

    /**
     * Has the same semantic as {@link SchedulingBucket#consume(long, ScheduledExecutorService)}
     */
    CompletableFuture<VerboseResult<Void>> consume(long numTokens, ScheduledExecutorService scheduler);

}
