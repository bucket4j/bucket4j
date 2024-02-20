package io.github.bucket4j.distributed.proxy;

import java.sql.PreparedStatement;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.TimeMeter;

public interface Timeout {

    Timeout NO_TIMEOUT = notSpecifiedTimeout();

    <T> T call(Function<Optional<Long>, T> timeBoundedOperation);

    void run(Consumer<Optional<Long>> timeBoundedOperation);

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
