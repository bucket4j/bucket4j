package org.isomorphism.util;

import org.junit.Test;

public class TokenBucketBuilderTest
{
  private final TokenBuckets.Builder builder = TokenBuckets.builder();

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeCapacity() {
    builder.withCapacity(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testZeroCapacity() {
    builder.withCapacity(0);
  }
}
