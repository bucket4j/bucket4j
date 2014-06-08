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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A token bucket implementation used for rate limiting access to a portion of code.  This implementation is that of a
 * leaky bucket in the sense that it has a finite capacity and any added tokens that would exceed this capacity will
 * "overflow" out of the bucket and are lost forever.
 * <p/>
 * In this implementation the rules for refilling the bucket are encapsulated in a provided {@code RefillStrategy}
 * instance.  Prior to attempting to consume any tokens the refill strategy will be consulted to see how many tokens
 * should be added to the bucket.
 * <p/>
 * In addition in this implementation the method of yielding CPU control is encapsulated in the provided
 * {@code SleepStrategy} instance.  For high performance applications where tokens are being refilled incredibly quickly
 * and an accurate bucket implementation is required, it may be useful to never yield control of the CPU and to instead
 * busy wait.  This strategy allows the caller to make this decision for themselves instead of the library forcing a
 * decision.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Token_bucket">Token Bucket on Wikipedia</a>
 * @see <a href="http://en.wikipedia.org/wiki/Leaky_bucket">Leaky Bucket on Wikipedia</a>
 */
public class TokenBucket
{
  private final long capacity;
  private final RefillStrategy refillStrategy;
  private final SleepStrategy sleepStrategy;
  private long size;

  public TokenBucket(long capacity, RefillStrategy refillStrategy, SleepStrategy sleepStrategy)
  {
    checkArgument(capacity > 0);

    this.capacity = capacity;
    this.refillStrategy = checkNotNull(refillStrategy);
    this.sleepStrategy = checkNotNull(sleepStrategy);
    this.size = 0;
  }

  /**
   * Attempt to consume a single token from the bucket.  If it was consumed then {@code true} is returned, otherwise
   * {@code false} is returned.
   *
   * @return {@code true} if a token was consumed, {@code false} otherwise.
   */
  public boolean tryConsume()
  {
    return tryConsume(1);
  }

  /**
   * Attempt to consume a specified number of tokens from the bucket.  If the tokens were consumed then {@code true}
   * is returned, otherwise {@code false} is returned.
   *
   * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
   * @return {@code true} if the tokens were consumed, {@code false} otherwise.
   */
  public synchronized boolean tryConsume(long numTokens)
  {
    checkArgument(numTokens > 0, "Number of tokens to consume must be positive");
    checkArgument(numTokens <= capacity, "Number of tokens to consume must be less than the capacity of the bucket.");

    // Give the refill strategy a chance to add tokens if it needs to
    long newTokens = Math.max(0, refillStrategy.refill());
    this.size = Math.max(0, Math.min(this.size + newTokens, capacity));

    // Now try to consume some tokens
    if (numTokens <= this.size) {
      this.size -= numTokens;
      return true;
    }

    return false;
  }

  /**
   * Consume a single token from the bucket.  If no token is currently available then this method will block until a
   * token becomes available.
   */
  public void consume()
  {
    consume(1);
  }

  /**
   * Consumes multiple tokens from the bucket.  If enough tokens are not currently available then this method will block
   * until
   *
   * @param numTokens The number of tokens to consume from teh bucket, must be a positive number.
   */
  public void consume(long numTokens)
  {
    while (true) {
      if (tryConsume(numTokens)) {
        break;
      }

      sleepStrategy.sleep();
    }
  }

  /** Encapsulation of a refilling strategy for a token bucket. */
  public static interface RefillStrategy
  {
    /**
     * Returns the number of tokens to add to the token bucket.
     *
     * @return The number of tokens to add to the token bucket.
     */
    long refill();
  }

  /** Encapsulation of a strategy for relinquishing control of the CPU. */
  public static interface SleepStrategy
  {
    /**
     * Sleep for a short period of time to allow other threads and system processes to execute.
     */
    void sleep();
  }
}
