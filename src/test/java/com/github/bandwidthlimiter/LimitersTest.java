package com.github.bandwidthlimiter;

import org.junit.Test;

import static com.github.bandwidthlimiter.bucket.TimeMeter.SYSTEM_MILLISECONDS;
import static com.github.bandwidthlimiter.bucket.TimeMeter.SYSTEM_NANOTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LimitersTest {

    @Test(expected=IllegalAccessException.class)
    public void testConstructorPrivate() throws Exception {
        Limiters.class.newInstance();
        fail("Utility class constructor should be private");
    }

    @Test
    public void testWithNanoTimePrecision() throws Exception {
        assertEquals(SYSTEM_NANOTIME, Limiters.withNanoTimePrecision().getTimeMeter());
    }

    @Test
    public void testWithMillisTimePrecision() throws Exception {
        assertEquals(SYSTEM_MILLISECONDS, Limiters.withMillisTimePrecision().getTimeMeter());
    }

    @Test
    public void testWithCustomTimePrecision() throws Exception {
        assertEquals(SYSTEM_NANOTIME, Limiters.withCustomTimePrecision(SYSTEM_NANOTIME).getTimeMeter());
        assertEquals(SYSTEM_MILLISECONDS, Limiters.withCustomTimePrecision(SYSTEM_MILLISECONDS).getTimeMeter());
    }

}