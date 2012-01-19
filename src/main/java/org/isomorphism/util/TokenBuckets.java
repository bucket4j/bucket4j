/*
 * Copyright 2012 Brandon Beck
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
package org.isomorphism.util;

import com.google.common.base.Ticker;

import java.util.concurrent.TimeUnit;

/** Static utility methods pertaining to creating {@link TokenBucket} instances. */
public final class TokenBuckets
{
  private TokenBuckets() {}

  /**
   * Construct a token bucket that uses a fixed interval refill strategy.  Initially the bucket will start with
   * {@code capacityTokens} tokens in it, and every {@code period} time units {@code refillTokens} will be added to
   * it.  The tokens are added all at one time on the interval boundaries.  By default the system clock is used for
   * keeping time.
   */
  public static TokenBucket newFixedIntervalRefill(long capacityTokens, long refillTokens, long period, TimeUnit unit)
  {
    Ticker ticker = Ticker.systemTicker();
    TokenBucket.RefillStrategy strategy = new FixedIntervalRefillStrategy(ticker, refillTokens, period, unit);
    return new TokenBucket(capacityTokens, strategy);
  }
}
