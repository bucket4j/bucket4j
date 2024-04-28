/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2022 Vladimir Bukhtoyarov
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
package io.github.bucket4j.benchmark.state;

import java.time.Duration;
import java.util.function.Supplier;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import io.github.resilience4j.ratelimiter.internal.SemaphoreBasedRateLimiter;

@State(Scope.Benchmark)
public class Resilience4jState {

    private final RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
            .limitForPeriod(Integer.MAX_VALUE / 2)
            .timeoutDuration(Duration.ofNanos(Long.MAX_VALUE / 2))
            .build();

    private final Supplier<String> stringSupplier = () -> {
        return "";
    };

    private final  RateLimiter semaphoreBasedRateLimiter = new SemaphoreBasedRateLimiter("semaphoreBased", rateLimiterConfig);
    private final  AtomicRateLimiter atomicRateLimiter = new AtomicRateLimiter("atomicBased", rateLimiterConfig);

    public final Supplier<String> semaphoreGuardedSupplier = RateLimiter.decorateSupplier(semaphoreBasedRateLimiter, stringSupplier);
    public final Supplier<String> atomicGuardedSupplier = RateLimiter.decorateSupplier(atomicRateLimiter, stringSupplier);
}
