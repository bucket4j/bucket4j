/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.bandwidthlimiter.tokenbucket;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class TokenBucketImplTest {

//    private static final long MAX_CAPACITY = 10;
//    private static final long period = 1000;
//    private static final TimeUnit timeUnit = TimeUnit.NANOSECONDS;
//
//
//    private RefillStrategy refillStrategy;
//
//    private TokenBucketImpl bucket;
//    private TokenBucketImpl halfBucket;
//    private TokenBucketImpl emptyBucket;
//
//    @Mock
//    private NanoTimeWrapper nanoTimeWrapper;
//
//    @Mock
//    private WaitingStrategy waitingStrategy;
//
//    @Before
//    public void initMocks() {
//        MockitoAnnotations.initMocks(this);
//
//        refillStrategy = new FixedIntervalRefillStrategy(MAX_CAPACITY, period, timeUnit);
//        bucket = new TokenBucketImpl(MAX_CAPACITY, MAX_CAPACITY, refillStrategy, waitingStrategy, nanoTimeWrapper);
//        halfBucket = new TokenBucketImpl(MAX_CAPACITY, MAX_CAPACITY / 2, refillStrategy, waitingStrategy, nanoTimeWrapper);
//        emptyBucket = new TokenBucketImpl(MAX_CAPACITY, 0, refillStrategy, waitingStrategy, nanoTimeWrapper);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testTryConsumeZeroTokens() {
//        bucket.tryConsumeSingleToken(0);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testTryConsumeNegativeTokens() {
//        bucket.tryConsumeSingleToken(-1);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testTryConsumeMoreThanCapacityTokens() {
//        bucket.tryConsumeSingleToken(MAX_CAPACITY + 1);
//    }
//
//    @Test
//    public void testInitiallyCapacityEmpty() {
//        assertFalse(emptyBucket.tryConsumeSingleToken());
//        assertFalse(emptyBucket.tryConsumeSingleToken(MAX_CAPACITY - 1));
//
//        assertTrue(bucket.tryConsumeSingleToken());
//        assertTrue(bucket.tryConsumeSingleToken(MAX_CAPACITY - 1));
//    }
//
//    @Test
//    public void testTryConsumeOneToken() {
//        assertFalse(emptyBucket.tryConsumeSingleToken());
//        assertTrue(halfBucket.tryConsumeSingleToken());
//        assertTrue(bucket.tryConsumeSingleToken());
//    }
//
//    @Test
//    public void testTryConsumeMoreTokensThanAreAvailable() {
//        assertFalse(halfBucket.tryConsumeSingleToken(MAX_CAPACITY / 2 + 1));
//
//        assertTrue(bucket.tryConsumeSingleToken(MAX_CAPACITY));
//        assertFalse(bucket.tryConsumeSingleToken());
//    }
//
//    @Test
//    public void testRefill() {
//        when(nanoTimeWrapper.nanoTime()).thenReturn(refillStrategy.nanosRequiredToRefill(MAX_CAPACITY));
//        assertTrue(emptyBucket.tryConsumeSingleToken());
//        assertTrue(halfBucket.tryConsumeSingleToken(MAX_CAPACITY / 2 + 1));
//
//        assertTrue(bucket.tryConsumeSingleToken(MAX_CAPACITY));
//
//        when(nanoTimeWrapper.nanoTime()).thenReturn(refillStrategy.nanosRequiredToRefill(MAX_CAPACITY) + 1);
//        assertFalse(bucket.tryConsumeSingleToken(MAX_CAPACITY));
//    }

}
