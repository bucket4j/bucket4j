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
package org.isomorphism.util;

import com.google.common.base.Ticker;
import com.google.common.util.concurrent.Uninterruptibles;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/** Static utility methods pertaining to creating {@link TokenBucketImpl} instances. */
public final class TokenBuckets
{
  private TokenBuckets() {}

  public static Builder builder()
  {
    return new Builder();
  }

  public static class Builder
  {
    private Long capacity = null;
    private TokenBucketImpl.RefillStrategy refillStrategy = null;
    private TokenBucketImpl.SleepStrategy sleepStrategy = YIELDING_SLEEP_STRATEGY;
    private final Ticker ticker = Ticker.systemTicker();

    public Builder withCapacity(long numTokens)
    {
      checkArgument(numTokens > 0, "Must specify a positive number of tokens");
      capacity = numTokens;
      return this;
    }

    /** Refill tokens at a fixed interval. */
    public Builder withFixedIntervalRefillStrategy(long refillTokens, long period, TimeUnit unit)
    {
      return withRefillStrategy(new FixedIntervalRefillStrategy(ticker, refillTokens, period, unit));
    }

    /** Use a user defined refill strategy. */
    public Builder withRefillStrategy(TokenBucket.RefillStrategy refillStrategy)
    {
      this.refillStrategy = checkNotNull(refillStrategy);
      return this;
    }

    /** Use a sleep strategy that will always attempt to yield the CPU to other processes. */
    public Builder withYieldingSleepStrategy()
    {
      return withSleepStrategy(YIELDING_SLEEP_STRATEGY);
    }

    /**
     * Use a sleep strategy that will not yield the CPU to other processes.  It will busy wait until more tokens become
     * available.
     */
    public Builder withBusyWaitSleepStrategy()
    {
      return withSleepStrategy(BUSY_WAIT_SLEEP_STRATEGY);
    }

    /** Use a user defined sleep strategy. */
    public Builder withSleepStrategy(TokenBucket.SleepStrategy sleepStrategy)
    {
      this.sleepStrategy = checkNotNull(sleepStrategy);
      return this;
    }

    /** Build the token bucket. */
    public TokenBucketImpl build()
    {
      checkNotNull(capacity, "Must specify a capacity");
      checkNotNull(refillStrategy, "Must specify a refill strategy");

      return new TokenBucketImpl(capacity, refillStrategy, sleepStrategy);
    }
  }

  private static final TokenBucketImpl.SleepStrategy YIELDING_SLEEP_STRATEGY = new TokenBucketImpl.SleepStrategy()
  {
    @Override
    public void sleep()
    {
      // Sleep for the smallest unit of time possible just to relinquish control
      // and to allow other threads to run.
      Uninterruptibles.sleepUninterruptibly(1, TimeUnit.NANOSECONDS);
    }
  };

  private static final TokenBucketImpl.SleepStrategy BUSY_WAIT_SLEEP_STRATEGY = new TokenBucketImpl.SleepStrategy()
  {
    @Override
    public void sleep()
    {
      // Do nothing, don't sleep.
    }
  };
}
