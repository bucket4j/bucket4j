package com.github.bandwidthlimiter.bucket;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class TimeMeterTest {

    @Test(expected = InterruptedException.class)
    public void sleepForMillisecondTimerShouldThrowExceptionWhenThreadInterrupted() throws InterruptedException {
        Thread.currentThread().interrupt();
        TimeMeter.SYSTEM_MILLISECONDS.sleep(TimeUnit.SECONDS.toMillis(10));
    }

    @Test(expected = InterruptedException.class)
    public void sleepForNanosecondTimerShouldThrowExceptionWhenThreadInterrupted() throws InterruptedException {
        Thread.currentThread().interrupt();
        TimeMeter.SYSTEM_NANOTIME.sleep(TimeUnit.SECONDS.toNanos(10));
    }

}