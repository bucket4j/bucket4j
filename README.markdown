Introduction
------------
This library provides an implementation of a token bucket algorithm which is useful for providing rate limited access
to a portion of code.  The implementation provided is that of a "leaky bucket" in the sense that the bucket has a finite
capacity and any added tokens that would exceed this capacity will "overflow" out of the bucket and be lost forever.

In this implementation the rules for refilling the bucket are encapsulated in a provided RefillStrategy instance.  Prior
to attempting to consume any tokens the refill strategy will be consulted to see how many tokens should be added to the
bucket

We use [Travis CI](http://about.travis-ci.org) for build verification.  [![Build Status](https://secure.travis-ci.org/bbeck/token-bucket.png?branch=master)](http://travis-ci.org/bbeck/token-bucket)

See also:

* [Wikipedia - Token Bucket](http://en.wikipedia.org/wiki/Token_bucket)
* [Wikipedia - Leaky Bucket](http://en.wikipedia.org/wiki/Leaky_bucket)

Usage
-----
Using a token bucket is incredibly easy and is best illustrated by an example.  Suppose you have a piece of code that
polls a website and you would only like to be able to access the site once per second:

```java
// Create a token bucket with a capacity of 1 token that refills at a fixed interval of 1 token/sec.
TokenBucket bucket = TokenBuckets.newFixedIntervalRefill(1, 1, 1, TimeUnit.SECONDS);

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
// Create a token bucket with a capacity of 40 kb tokens that refills at a fixed interval of 20 kb tokens per second
TokenBucket bucket = TokenBuckets.newFixedIntervalRefill(40960, 20480, 1, TimeUnit.SECONDS);

// ...

while (true) {
  String response = prepareResponse();

  // Consume tokens from the bucket commensurate with the size of the response
  bucket.consume(response.length());

  send(response);
}
```

Maven Setup
-----------
The token bucket library is distributed through maven central.  Just include it as a dependency in your ```pom.xml```.

```xml
<dependency>
    <groupId>org.isomorphism</groupId>
    <artifactId>token-bucket</artifactId>
    <version>1.2</version>
</dependency>
```

License
-------
Copyright 2012 Brandon Beck
Licensed under the Apache Software License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.
