package io.github.bucket4j.distributed.proxy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import io.github.bucket4j.TimeoutException;
import io.github.bucket4j.mock.TimeMeterMock;

import static org.junit.jupiter.api.Assertions.*;

public class TimeoutTest {

    private static Duration requestTimeout = Duration.ofSeconds(1);
    private TimeMeterMock clock = new TimeMeterMock();
    private ProxyManagerConfig proxyManagerConfig = ProxyManagerConfig.getDefault()
        .withClientClock(clock)
        .withRequestTimeout(requestTimeout);
    private Timeout timeout = Timeout.of(proxyManagerConfig);


    @Test
    public void testRun() {
        timeout.run(timeoutNanos -> assertEquals(requestTimeout.toNanos(), timeoutNanos.get()));

        clock.addTime(requestTimeout.toNanos() / 2);
        timeout.run(timeoutNanos -> assertEquals(requestTimeout.toNanos() / 2 , timeoutNanos.get()));

        try {
            clock.addTime(requestTimeout.toNanos() / 2 + 1);
            timeout.run(timeoutNanos -> {throw new RuntimeException("should not be called");});
            fail("should be time-outed");
        } catch (TimeoutException e) {
            assertEquals(requestTimeout.toNanos(), e.getRequestTimeoutNanos());
            assertEquals(requestTimeout.toNanos() + 1, e.getNanosElapsed());
        }
    }

    @Test
    public void testCall() {
        timeout.call(timeoutNanos -> {
            assertEquals(requestTimeout.toNanos(), timeoutNanos.get());
            return null;
        });

        clock.addTime(requestTimeout.toNanos() / 2);
        timeout.call(timeoutNanos -> {
            assertEquals(requestTimeout.toNanos() / 2, timeoutNanos.get());
            return null;
        });

        try {
            clock.addTime(requestTimeout.toNanos() / 2 + 1);
            timeout.call(timeoutNanos -> {
                throw new RuntimeException("should not be called");
            });
            fail("should be time-outed");
        } catch (TimeoutException e) {
            assertEquals(requestTimeout.toNanos(), e.getRequestTimeoutNanos());
            assertEquals(requestTimeout.toNanos() + 1, e.getNanosElapsed());
        }
    }

    @Test
    public void testCallAsync() throws ExecutionException, InterruptedException {
        timeout.callAsync(timeoutNanos -> {
            assertEquals(requestTimeout.toNanos(), timeoutNanos.get());
            return null;
        });

        clock.addTime(requestTimeout.toNanos() / 2);
        timeout.callAsync(timeoutNanos -> {
            assertEquals(requestTimeout.toNanos() / 2, timeoutNanos.get());
            return null;
        });


        clock.addTime(requestTimeout.toNanos() / 2 + 1);
        CompletableFuture<TimeoutException> future = timeout.callAsync(timeoutNanos -> {
            throw new RuntimeException("should not be called");
        });
        assertTrue(future.isCompletedExceptionally());
        TimeoutException e = future.exceptionally(ex -> (TimeoutException) ex).get();
        assertEquals(requestTimeout.toNanos(), e.getRequestTimeoutNanos());
        assertEquals(requestTimeout.toNanos() + 1, e.getNanosElapsed());
    }

}