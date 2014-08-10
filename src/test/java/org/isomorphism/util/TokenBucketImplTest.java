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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class TokenBucketImplTest
{
  private static final long CAPACITY = 10;

  private final MockRefillStrategy refillStrategy = new MockRefillStrategy();
  private final TokenBucket.SleepStrategy sleepStrategy = mock(TokenBucket.SleepStrategy.class);
  private final TokenBucketImpl bucket = new TokenBucketImpl(CAPACITY, refillStrategy, sleepStrategy);

  @Test(expected = IllegalArgumentException.class)
  public void testTryConsumeZeroTokens()
  {
    bucket.tryConsume(0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTryConsumeNegativeTokens()
  {
    bucket.tryConsume(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTryConsumeMoreThanCapacityTokens()
  {
    bucket.tryConsume(100);
  }

  @Test
  public void testBucketInitiallyEmpty()
  {
    assertFalse(bucket.tryConsume());
  }

  @Test
  public void testTryConsumeOneToken()
  {
    refillStrategy.addToken();
    assertTrue(bucket.tryConsume());
  }

  @Test
  public void testTryConsumeMoreTokensThanAreAvailable()
  {
    refillStrategy.addToken();
    assertFalse(bucket.tryConsume(2));
  }

  @Test
  public void testTryRefillMoreThanCapacityTokens()
  {
    refillStrategy.addTokens(CAPACITY + 1);
    assertTrue(bucket.tryConsume(CAPACITY));
    assertFalse(bucket.tryConsume(1));
  }

  @Test
  public void testTryRefillWithTooManyTokens()
  {
    refillStrategy.addTokens(CAPACITY);
    assertTrue(bucket.tryConsume());

    refillStrategy.addTokens(Long.MAX_VALUE);
    assertTrue(bucket.tryConsume(CAPACITY));
    assertFalse(bucket.tryConsume(1));
  }

  private static final class MockRefillStrategy implements TokenBucketImpl.RefillStrategy
  {
    private long numTokensToAdd = 0;

    public long refill()
    {
      long numTokens = numTokensToAdd;
      numTokensToAdd = 0;
      return numTokens;
    }

    public void addToken()
    {
      numTokensToAdd++;
    }

    public void addTokens(long numTokens)
    {
      numTokensToAdd += numTokens;
    }
  }
}
