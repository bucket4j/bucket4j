/*
 * Copyright 2012-2014 Brandon Beck
 *
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
 */
package ru.vbukhtoyarov.concurrency.tokenbucket;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class FixedIntervalRefillStrategyTest {
    private static final long N = 5;                     // 5 tokens
    private static final long P = 10;                    // every 10
    private static final TimeUnit U = TimeUnit.SECONDS;  // seconds

    private final MockTicker ticker = new MockTicker();
    private final FixedIntervalRefillStrategy strategy = new FixedIntervalRefillStrategy(ticker, N, P, U);

    @Test
    public void testFirstRefill() {
        assertEquals(N, strategy.refill());
    }

    @Test
    public void testNoRefillUntilPeriodUp() {
        strategy.refill();

        // Another refill shouldn't come for P units.
        for (int i = 0; i < P - 1; i++) {
            ticker.advance(1, U);
            assertEquals(0, strategy.refill());
        }
    }

    @Test
    public void testRefillEveryPeriod() {
        for (int i = 0; i < 10; i++) {
            assertEquals(N, strategy.refill());
            ticker.advance(P, U);
        }
    }

    private static final class MockTicker extends NanoTimeWrapper {
        private long now = 0;

        @Override
        public long nanoTime() {
            return now;
        }

        public void advance(long delta, TimeUnit unit) {
            now += unit.toNanos(delta);
        }
    }
}
