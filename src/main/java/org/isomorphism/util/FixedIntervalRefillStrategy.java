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

/**
 * A token bucket refill strategy that will provide N tokens for a token bucket to consume every T units of time.
 * The tokens are refilled in bursts rather than at a fixed rate.  This refill strategy will never allow more than
 * N tokens to be consumed during a window of time T.
 */
public class FixedIntervalRefillStrategy implements TokenBucket.RefillStrategy
{
  private final Ticker ticker;
  private final long numTokens;
  private final long periodInNanos;
  private long nextRefillTime;

  /**
   * Create a FixedIntervalRefillStrategy.
   *
   * @param ticker    A ticker to use to measure time.
   * @param numTokens The number of tokens to add to the bucket every interval.
   * @param period    How often to refill the bucket.
   * @param unit      Unit for period.
   */
  public FixedIntervalRefillStrategy(Ticker ticker, long numTokens, long period, TimeUnit unit)
  {
    this.ticker = ticker;
    this.numTokens = numTokens;
    this.periodInNanos = unit.toNanos(period);
    this.nextRefillTime = -1;
  }

  public synchronized long refill()
  {
    long now = ticker.read();
    if (now < nextRefillTime) {
      return 0;
    }
    nextRefillTime = now + periodInNanos;
    return numTokens;
  }
}

