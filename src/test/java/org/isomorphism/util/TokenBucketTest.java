package org.isomorphism.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TokenBucketTest
{
  private static final long CAPACITY = 10;

  private final MockRefillStrategy refillStrategy = new MockRefillStrategy();
  private final TokenBucket bucket = new TokenBucket(CAPACITY, refillStrategy);

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

  private static final class MockRefillStrategy implements TokenBucket.RefillStrategy
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
  }
}
