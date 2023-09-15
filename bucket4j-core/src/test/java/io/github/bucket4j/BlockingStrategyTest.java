package io.github.bucket4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BlockingStrategyTest {

    @Test
    @Timeout(1)
    public void sleepShouldThrowExceptionWhenThreadInterrupted() throws InterruptedException {
        Assertions.assertThrows(InterruptedException.class, () -> {
            Thread.currentThread().interrupt();
            BlockingStrategy.PARKING.park(TimeUnit.SECONDS.toNanos(10));
        });
    }

    @Test
    @Timeout(3)
    public void sleepShouldHandleSpuriousWakeup() throws InterruptedException {
        // two lines bellow lead to spurious wakeup at first invocation of park
        Thread.currentThread().interrupt();
        Thread.interrupted();

        long startNanos = System.nanoTime();
        long nanosToPark = TimeUnit.SECONDS.toNanos(1);
        BlockingStrategy.PARKING.park(nanosToPark);
        assertTrue(System.nanoTime() - startNanos >= nanosToPark);
    }

    @Test
    @Timeout(10)
    public void sleepUniterruptibleShouldNotThrowInterruptedException() {
        long nanosToPark = TimeUnit.SECONDS.toNanos(1);
        Thread.currentThread().interrupt();
        long startNanos = System.nanoTime();
        UninterruptibleBlockingStrategy.PARKING.parkUninterruptibly(nanosToPark);
        assertTrue(System.nanoTime() - startNanos >= nanosToPark);
        assertTrue(Thread.currentThread().isInterrupted());
    }

}