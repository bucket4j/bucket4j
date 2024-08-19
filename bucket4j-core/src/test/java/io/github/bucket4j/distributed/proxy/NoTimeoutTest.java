package io.github.bucket4j.distributed.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class NoTimeoutTest {

    private Timeout timeout = Timeout.of(ProxyManagerConfig.getDefault());

    @Test
    public void testRun() {
        timeout.run(timeoutNanos -> assertTrue(timeoutNanos.isEmpty()));
    }

    @Test
    public void testCall() {
        timeout.call(timeoutNanos -> {
            assertTrue(timeoutNanos.isEmpty());
            return null;
        });
    }

    @Test
    public void testCallAsync() {
        timeout.callAsync(timeoutNanos -> {
            assertTrue(timeoutNanos.isEmpty());
            return null;
        });
    }

}