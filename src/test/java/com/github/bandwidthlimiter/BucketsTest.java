package com.github.bandwidthlimiter;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.bandwidthlimiter.bucket.TimeMeter.SYSTEM_MILLISECONDS;
import static com.github.bandwidthlimiter.bucket.TimeMeter.SYSTEM_NANOTIME;
import static org.junit.Assert.assertEquals;

public class BucketsTest {

    @Test
    public void testWithNanoTimePrecision() throws Exception {
        assertEquals(SYSTEM_NANOTIME, Buckets.withNanoTimePrecision().getTimeMeter());
    }

    @Test
    public void testWithMillisTimePrecision() throws Exception {
        assertEquals(SYSTEM_MILLISECONDS, Buckets.withMillisTimePrecision().getTimeMeter());
    }

    @Test
    public void testWithCustomTimePrecision() throws Exception {
        assertEquals(SYSTEM_NANOTIME, Buckets.withCustomTimePrecision(SYSTEM_NANOTIME).getTimeMeter());
        assertEquals(SYSTEM_MILLISECONDS, Buckets.withCustomTimePrecision(SYSTEM_MILLISECONDS).getTimeMeter());
    }

    @Test
    public void testToString() throws Exception {
        Buckets.withMillisTimePrecision().withLimitedBandwidth(100, TimeUnit.MILLISECONDS.toMillis(10)).build().toString();
    }

}