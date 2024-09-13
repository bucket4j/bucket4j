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
public interface AsyncExecutionStrategy {

    <T> CompletableFuture<T>  executeAsync(Supplier<CompletableFuture<T>> supplier);

    /**
     * This execution strategy always communicates with backend in current thread
     */
    AsyncExecutionStrategy SAME_TREAD = new AsyncExecutionStrategy() {
        @Override
        public <T> CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> supplier) {
            return supplier.get();
        }
    };

    /**
     * This execution strategy always communicates with backend in specified executor
     */
    static AsyncExecutionStrategy background(Executor executor) {
        return new AsyncExecutionStrategy() {
            @Override
            public <T> CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> supplier) {
                CompletableFuture<CompletableFuture<T>> futureToFuture = CompletableFuture.supplyAsync(supplier, executor);

                CompletableFuture<T> resultFuture = new CompletableFuture<>();
                futureToFuture.whenComplete((CompletableFuture<T> mediateFuture, Throwable error) -> {
                    if (error != null) {
                        resultFuture.completeExceptionally(error);
                    } else {
                        mediateFuture.whenComplete((T result, Throwable err) -> {
                            if (err != null) {
                                resultFuture.completeExceptionally(err);
                            } else {
                                resultFuture.complete(result);
                            }
                        });
                    }
                });

                return resultFuture;
            }
        };
    }

    /**
     * This execution strategy always communicates with backend in specified executor
     */
    static AsyncExecutionStrategy backgroundTimeBounded(Executor executor, Duration timeout) {
        long timeoutNanos = timeout.toNanos();
        if (timeoutNanos <= 0) {
            throw new IllegalStateException("timeout should be positive");
        }
        return new AsyncExecutionStrategy() {
            @Override
            public <T> CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> supplier) {
                CompletableFuture<CompletableFuture<T>> futureToFuture = CompletableFuture.supplyAsync(supplier, executor);

                CompletableFuture<T> resultFuture = new CompletableFuture<>();
                futureToFuture.whenComplete((CompletableFuture<T> mediateFuture, Throwable error) -> {
                    if (error != null) {
                        resultFuture.completeExceptionally(error);
                    } else {
                        mediateFuture.whenComplete((T result, Throwable err) -> {
                            if (err != null) {
                                resultFuture.completeExceptionally(err);
                            } else {
                                resultFuture.complete(result);
                            }
                        });
                    }
                });

                return resultFuture.orTimeout(timeoutNanos, TimeUnit.NANOSECONDS);
            }
        };
    }

}
