Introduction
------------
This library provides an implementation of a token bucket algorithm which is useful for providing rate limited access
to a portion of code.  The implementation provided is that of a "leaky bucket" in the sense that the bucket has a finite
capacity and any added tokens that would exceed this capacity will "overflow" out of the bucket and be lost forever.

In this implementation the rules for refilling the bucket are encapsulated in a provided RefillStrategy instance.  Prior
to attempting to consume any tokens the refill strategy will be consulted to see how many tokens should be added to the
bucket

See also:
* [Wikipedia - Token Bucket](http://en.wikipedia.org/wiki/Token_bucket)
* [Wikipedia - Leaky Bucket](http://en.wikipedia.org/wiki/Leaky_bucket)

Usage
-----
Using a token bucket is incredibly easy and is best illustrated by an example.  Suppose you have a piece of code that
polls a website and you would only like to be able to access the site once per second:

```java
// Use the system ticker for measuring time (part of Guava)
Ticker ticker = Ticker.systemTicker()

// Refill the bucket with 1 token every 1 second
RefillStrategy refillStrategy = new FixedIntervalRefillStrategy(ticker, 1, 1, TimeUnit.SECONDS);

// Create a token bucket with a capacity of 1 token
TokenBucket bucket = new TokenBucket(1, refillStrategy);

// ...

while (true) {
  // Consume a token from the token bucket.  If a token is not available this method will block until
  // the refill strategy adds one to the bucket.
  bucket.consume(1);

  poll();
}
```

As another example suppose you wanted to rate limit the size response of a server to the client to 20 kb/sec but want to
allow for a periodic burst rate of 40 kb/sec:

```java
// Use the system ticker for measuring time (part of Guava)
Ticker ticker = Ticker.systemTicker()

// Refill the bucket with 20 kb of tokens per second
RefillStrategy refillStrategy = new FixedIntervalRefillStrategy(ticker, 20480, 1, TimeUnit.SECONDS);

// Create a token bucket with a capacity of 40 kb tokens
TokenBucket bucket = new TokenBucket(40960, refillStrategy);

// ...

while (true) {
  String response = prepareResponse();

  // Consume tokens from the bucket commensurate with the size of the response
  bucket.consume(response.length());

  send(response);
}
```