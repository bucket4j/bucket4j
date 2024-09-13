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
package io.github.bucket4j.distributed.proxy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.github.bucket4j.BucketExceptions;

/**
 * Defines the strategy for request execution.
 */
public interface ExecutionStrategy {

    <T> T execute(Supplier<T> supplier);

    /**
     * This execution strategy always communicates with backend in current thread
     */
    ExecutionStrategy SAME_TREAD = new ExecutionStrategy() {
        @Override
        public <T> T execute(Supplier<T> supplier) {
            return supplier.get();
        }
    };

    /**
     * This execution strategy always communicates with backend in specified executor
     */
    static ExecutionStrategy background(Executor executor) {
        return new ExecutionStrategy() {
            @Override
            public <T> T execute(Supplier<T> supplier) {
                try {
                    return CompletableFuture.supplyAsync(supplier, executor).get();
                } catch (Throwable e) {
                    throw BucketExceptions.executionException(e);
                }
            }
        };
    }

    /**
     * This execution strategy always communicates with backend in specified executor
     */
    static ExecutionStrategy backgroundTimeBounded(Executor executor, Duration timeout) {
        long timeoutNanos = timeout.toNanos();
        if (timeoutNanos <= 0) {
            throw new IllegalStateException("timeout should be positive");
        }
        return new ExecutionStrategy() {
            @Override
            public <T> T execute(Supplier<T> supplier) {
                try {
                    return CompletableFuture.supplyAsync(supplier, executor)
                            .get(timeoutNanos, TimeUnit.NANOSECONDS);
                } catch (Throwable e) {
                    throw BucketExceptions.executionException(e);
                }
            }
        };
    }

}
