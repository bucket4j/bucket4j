/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2024 Vladimir Bukhtoyarov
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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.TimeMeter;

public interface Timeout {

    Timeout NO_TIMEOUT = notSpecifiedTimeout();

    <T> T call(Function<Optional<Long>, T> timeBoundedOperation);

    void run(Consumer<Optional<Long>> timeBoundedOperation);

    <T> CompletableFuture<T> callAsync(Function<Optional<Long>, CompletableFuture<T>> timeBoundedOperation);

    static Timeout of(ClientSideConfig clientSideConfig) {
        Optional<Long> requestTimeout = clientSideConfig.getRequestTimeoutNanos();
        if (requestTimeout.isEmpty()) {
            return NO_TIMEOUT;
        } else {
            TimeMeter clientClock = clientSideConfig.getClientSideClock().orElse(TimeMeter.SYSTEM_NANOTIME);
            return boundedTimeout(clientClock, requestTimeout.get());
        }
    }

    static Timeout notSpecifiedTimeout() {
        return new Timeout() {
            @Override
            public <T> T call(Function<Optional<Long>, T> timeBoundedOperation) {
                return timeBoundedOperation.apply(Optional.empty());
            }

            @Override
            public void run(Consumer<Optional<Long>> timeBoundedOperation) {
                timeBoundedOperation.accept(Optional.empty());
            }

            @Override
            public <T> CompletableFuture<T> callAsync(Function<Optional<Long>, CompletableFuture<T>> timeBoundedOperation) {
                return timeBoundedOperation.apply(Optional.empty());
            }
        };
    }

    static Timeout boundedTimeout(TimeMeter clientClock, long requestTimeoutNanos) {
        long startNanos = clientClock.currentTimeNanos();
        return new Timeout() {
            @Override
            public <T> T call(Function<Optional<Long>, T> timeBoundedOperation) {
                long nanosElapsed = clientClock.currentTimeNanos() - startNanos;
                if (nanosElapsed >= requestTimeoutNanos) {
                    throw BucketExceptions.timeoutReached(nanosElapsed, requestTimeoutNanos);
                }
                long remainingLimitNanos = requestTimeoutNanos - nanosElapsed;
                return timeBoundedOperation.apply(Optional.of(remainingLimitNanos));
            }

            @Override
            public <T> CompletableFuture<T> callAsync(Function<Optional<Long>, CompletableFuture<T>> timeBoundedOperation) {
                long nanosElapsed = clientClock.currentTimeNanos() - startNanos;
                if (nanosElapsed >= requestTimeoutNanos) {
                    CompletableFuture<T> timeouted = new CompletableFuture<>();
                    timeouted.completeExceptionally(BucketExceptions.timeoutReached(nanosElapsed, requestTimeoutNanos));
                    return timeouted;
                }
                long remainingLimitNanos = requestTimeoutNanos - nanosElapsed;
                return timeBoundedOperation.apply(Optional.of(remainingLimitNanos));
            }

            @Override
            public void run(Consumer<Optional<Long>> timeBoundedOperation) {
                long nanosElapsed = clientClock.currentTimeNanos() - startNanos;
                if (nanosElapsed >= requestTimeoutNanos) {
                    throw BucketExceptions.timeoutReached(nanosElapsed, requestTimeoutNanos);
                }
                long remainingLimitNanos = requestTimeoutNanos - nanosElapsed;
                timeBoundedOperation.accept(Optional.of(remainingLimitNanos));
            }
        };
    }

}
